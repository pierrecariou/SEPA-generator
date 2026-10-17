#!/usr/bin/env python3
"""Exercise desktop staging, or inspect an actual .deb with --deb FILE.

Uses only Python's standard library, Bash, and (for --deb) native Debian tools.
Native desktop/AppStream validators are also run when installed (required in CI).
"""
import configparser
import hashlib
import os
from pathlib import Path
import shutil
import struct
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET


REPO = Path(__file__).resolve().parents[3]
PROPS = dict(line.split("=", 1) for line in
             (REPO / "packaging/edition.properties").read_text(encoding="utf-8").splitlines()
             if line and not line.startswith("#"))
PACKAGE = PROPS["LINUX_PACKAGE_NAME"]
APP_NAME = PROPS["APP_NAME"]
DESKTOP_ID = PACKAGE + "-" + APP_NAME.replace(" ", "_") + ".desktop"
DEB = None
if "--deb" in sys.argv:
    index = sys.argv.index("--deb")
    DEB = Path(sys.argv[index + 1]).resolve()
    del sys.argv[index:index + 2]


def stage(root, repo=REPO):
    subprocess.run(["bash", (repo / "packaging/community/linux-desktop.sh").as_posix(),
                    repo.as_posix(), root.as_posix()], check=True)


def fixture(root):
    """The desktop layout observed in the published jpackage 23 DEB."""
    app = root / "opt" / PACKAGE / "lib"
    app.mkdir(parents=True)
    (app / DESKTOP_ID).write_text(
        "[Desktop Entry]\nName=" + APP_NAME + "\nComment=" + PROPS["DESCRIPTION"] +
        '\nExec="/opt/' + PACKAGE + '/bin/' + APP_NAME + '"\n' +
        "Icon=/opt/" + PACKAGE + "/lib/SEPA_Generator_Community.png\n" +
        "Terminal=false\nType=Application\nCategories=Office;Finance\nMimeType=\n",
        encoding="utf-8")


class LinuxMetadataTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix="sepa desktop test ")
        cls.addClassCleanup(cls.temp.cleanup)
        cls.root = Path(cls.temp.name)
        if DEB:
            subprocess.run(["dpkg-deb", "--raw-extract", str(DEB), str(cls.root)], check=True)
        else:
            fixture(cls.root)
            stage(cls.root)
        cls.desktop_path = cls.root / "usr/share/applications" / DESKTOP_ID
        desktop = configparser.ConfigParser(interpolation=None, strict=True)
        desktop.read(cls.desktop_path, encoding="utf-8")
        cls.desktop = desktop["Desktop Entry"]
        cls.metainfo_path = (cls.root / "usr/share/metainfo" /
                             (PROPS["MAC_PACKAGE_IDENTIFIER"] + ".metainfo.xml"))
        cls.metainfo = ET.parse(cls.metainfo_path).getroot()

    def test_public_launcher_identity(self):
        self.assertEqual(self.desktop["Name"], "SEPA Generator Community")
        self.assertEqual(self.desktop["Comment"], PROPS["DESCRIPTION"])
        self.assertEqual(self.desktop["Type"], "Application")
        self.assertEqual(self.desktop["Terminal"], "false")
        self.assertEqual(self.desktop["Categories"], "Office;Finance;")
        for field in ("Name", "Comment"):
            self.assertNotIn("pcariou", self.desktop[field].lower())
            self.assertNotIn("Pierre Cariou", self.desktop[field])

    def test_released_launcher_path_and_quoting(self):
        self.assertEqual(self.desktop["Exec"],
                         '"/opt/sepa-generator-community/bin/SEPA Generator Community"')
        if DEB:
            launcher = self.root / self.desktop["Exec"].strip('"').lstrip("/")
            self.assertTrue(launcher.is_file())
            self.assertTrue(os.access(launcher, os.X_OK))

    def test_running_window_matches_java_class(self):
        self.assertEqual(self.desktop["StartupWMClass"], "com-pcariou-generator-Generator")
        if DEB:
            cfg = self.root / "opt" / PACKAGE / "lib/app" / (APP_NAME + ".cfg")
            self.assertIn("app.mainclass=" + PROPS["MAIN_CLASS"], cfg.read_text())

    def test_one_menu_entry_and_compatible_xdg_source(self):
        entries = list((self.root / "usr/share/applications").glob("*.desktop"))
        self.assertEqual([p.name for p in entries], [DESKTOP_ID])
        source = self.root / "opt" / PACKAGE / "lib" / DESKTOP_ID
        self.assertEqual(source.read_bytes(), self.desktop_path.read_bytes())

    def test_icons_resolve_with_real_png_dimensions_and_artwork(self):
        self.assertEqual(self.desktop["Icon"], PACKAGE)
        for size in (16, 32, 48, 64, 256):
            with self.subTest(size=size):
                icon = (self.root / "usr/share/icons/hicolor" / f"{size}x{size}" /
                        "apps" / (PACKAGE + ".png"))
                data = icon.read_bytes()
                self.assertEqual(data[:8], b"\x89PNG\r\n\x1a\n")
                self.assertEqual(struct.unpack(">II", data[16:24]), (size, size))
                source = REPO / "view/src/main/resources" / f"sepa-generator-icon-app-bg-{size}.png"
                self.assertEqual(hashlib.sha256(data).digest(),
                                 hashlib.sha256(source.read_bytes()).digest())

    def test_appstream_links_to_shipped_desktop_and_icon(self):
        self.assertEqual(self.metainfo.attrib["type"], "desktop-application")
        self.assertEqual(self.metainfo.findtext("id"), "com.pcariou.sepa.community")
        launchable = self.metainfo.find("launchable")
        self.assertEqual(launchable.attrib["type"], "desktop-id")
        self.assertEqual(launchable.text, self.desktop_path.name)
        self.assertEqual(self.metainfo.findtext("icon"), self.desktop["Icon"])
        self.assertEqual(self.metainfo.find("icon").attrib["type"], "stock")

    def test_appstream_public_identity_and_licenses(self):
        self.assertEqual(self.metainfo.findtext("name"), "SEPA Generator Community")
        self.assertEqual(self.metainfo.findtext("developer/name"), "Niryosys")
        self.assertEqual(self.metainfo.findtext("url[@type='homepage']"), PROPS["HOMEPAGE"])
        self.assertEqual(self.metainfo.findtext("metadata_license"), "CC0-1.0")
        self.assertEqual(self.metainfo.findtext("project_license"), "Apache-2.0")
        self.assertIn("published by Niryosys", self.metainfo.findtext("description/p"))

    @unittest.skipUnless(DEB, "requires --deb artifact")
    def test_actual_debian_control_and_maintainer_hooks(self):
        control = subprocess.check_output(["dpkg-deb", "--field", str(DEB)], text=True)
        self.assertIn("Package: sepa-generator-community\n", control)
        self.assertIn("Maintainer: Niryosys <contact@sepa-xml-generator.com>\n", control)
        self.assertIn("Homepage: " + PROPS["HOMEPAGE"] + "\n", control)
        version = ET.parse(REPO / "pom.xml").findtext(
            "{*}properties/{*}revision")
        self.assertIn("Version: " + version + "\n", control)
        for script in ("postinst", "prerm"):
            content = (self.root / "DEBIAN" / script).read_text()
            self.assertIn("/opt/" + PACKAGE + "/lib/" + DESKTOP_ID, content)

    def test_native_metadata_validators(self):
        if not any(shutil.which(name) for name in ("desktop-file-validate", "appstreamcli")):
            self.skipTest("native desktop/AppStream validators unavailable")
        commands = [
            ("desktop-file-validate", [str(self.desktop_path)]),
            ("appstreamcli", ["validate", "--no-net", str(self.metainfo_path)]),
        ]
        for name, args in commands:
            with self.subTest(validator=name):
                if shutil.which(name):
                    subprocess.run([name] + args, check=True)
                else:
                    print(f"SKIP native validator: {name} unavailable")


@unittest.skipIf(DEB, "staging behavior is tested separately from the artifact")
class StagingTest(unittest.TestCase):
    def test_missing_jpackage_desktop_fails(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(subprocess.CalledProcessError):
                stage(Path(tmp))

    def test_repeated_staging_keeps_single_window_class(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            fixture(root)
            stage(root)
            stage(root)
            desktop = root / "usr/share/applications" / DESKTOP_ID
            self.assertEqual(desktop.read_text().count("StartupWMClass="), 1)

    def test_publisher_text_is_xml_escaped(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            repo = root / "repo with spaces"
            (repo / "packaging/community").mkdir(parents=True)
            shutil.copy(REPO / "packaging/community/linux-desktop.sh", repo / "packaging/community")
            props = (REPO / "packaging/edition.properties").read_text(encoding="utf-8")
            (repo / "packaging/edition.properties").write_text(
                props.replace("VENDOR=Niryosys", "VENDOR=Niryosys & <Publisher>"), encoding="utf-8")
            shutil.copytree(REPO / "view/src/main/resources", repo / "view/src/main/resources")
            fixture(root)
            stage(root, repo)
            meta = ET.parse(next((root / "usr/share/metainfo").glob("*.xml")))
            self.assertEqual(meta.findtext("developer/name"), "Niryosys & <Publisher>")


if __name__ == "__main__":
    unittest.main(verbosity=2)
