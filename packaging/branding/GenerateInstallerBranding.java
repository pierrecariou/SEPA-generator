/*
 * Generates the restrained SEPA Generator Community installer artwork that
 * replaces jpackage/WiX's generic default images.
 *
 * The generated files are COMMITTED to the repository (they are small, stable
 * and must be identical on every runner); this generator is kept so the artwork
 * is reproducible and reviewable rather than an opaque binary blob.
 *
 * Run from the repository root with a JDK 17+:
 *
 *   java packaging/branding/GenerateInstallerBranding.java
 *
 * Outputs:
 *   packaging/windows/branding/banner.bmp        493x58   WiX top banner
 *   packaging/windows/branding/dialog.bmp        493x312  WiX welcome/exit panel
 *   packaging/macos/branding/dmg-background.png  520x340  DMG volume background
 *
 * Brand rules (do not "improve" these without a brand decision):
 *   product   : SEPA Generator          edition : Community Edition
 *   publisher : Niryosys                navy    : #082B4B   cream : #FDF6EC
 *
 * Layout constraints that are NOT free choices:
 *   - WiX draws the dialog title/description as BLACK text over the top-left of
 *     banner.bmp and over the right-hand side of dialog.bmp, so those regions
 *     must stay light (cream). Only the reserved bands are navy.
 *   - The DMG background must be 520x340 because jpackage's stock DMGsetup.scpt
 *     sets the Finder window bounds to {400,100,920,440} and positions the .app
 *     at (120,130) and the Applications alias at (390,130) with 128px icons.
 */
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class GenerateInstallerBranding {

    private static final Color NAVY       = new Color(0x08, 0x2B, 0x4B);
    private static final Color CREAM      = new Color(0xFD, 0xF6, 0xEC);
    /** Cream at reduced emphasis, for secondary text on navy. */
    private static final Color CREAM_SOFT = new Color(0xC9, 0xD6, 0xE2);

    private static final String PRODUCT   = "SEPA Generator";
    private static final String EDITION   = "Community Edition";
    private static final String PUBLISHER = "Niryosys";

    public static void main(String[] args) throws Exception {
        Path repoRoot = Path.of("").toAbsolutePath();
        Path iconPng = repoRoot.resolve("packaging/macos/sepa-generator-1024.png");
        if (!Files.isRegularFile(iconPng)) {
            throw new IllegalStateException("Run this from the repository root; icon not found: " + iconPng);
        }
        BufferedImage icon = ImageIO.read(iconPng.toFile());

        write(windowsBanner(icon), "bmp", repoRoot.resolve("packaging/windows/branding/banner.bmp"));
        write(windowsDialog(icon), "bmp", repoRoot.resolve("packaging/windows/branding/dialog.bmp"));
        write(dmgBackground(icon), "png", repoRoot.resolve("packaging/macos/branding/dmg-background.png"));
    }

    // -------------------------------------------------------------------------
    // Windows: WiX banner strip shown at the top of every wizard page except the
    // first and last. The left half stays cream because WiX paints the page
    // title and description there in black.
    // -------------------------------------------------------------------------
    private static BufferedImage windowsBanner(BufferedImage icon) {
        int w = 493, h = 58;
        BufferedImage img = newImage(w, h, CREAM);
        Graphics2D g = graphics(img);

        // Navy hairline along the bottom edge: the single unifying brand mark.
        g.setColor(NAVY);
        g.fillRect(0, h - 3, w, 3);

        // Product icon on the right, clear of the WiX text area.
        int size = 34;
        drawIcon(g, icon, w - size - 18, (h - 3 - size) / 2, size);

        g.dispose();
        return img;
    }

    // -------------------------------------------------------------------------
    // Windows: welcome/exit panel. A navy spine on the left carries the identity;
    // the rest stays cream because WiX paints its text over the right-hand side.
    // -------------------------------------------------------------------------
    private static BufferedImage windowsDialog(BufferedImage icon) {
        int w = 493, h = 312, band = 170;
        BufferedImage img = newImage(w, h, CREAM);
        Graphics2D g = graphics(img);

        g.setColor(NAVY);
        g.fillRect(0, 0, band, h);

        drawIcon(g, icon, (band - 76) / 2, 54, 76);

        centered(g, PRODUCT, font(Font.BOLD, 16), CREAM, band, 164);
        centered(g, EDITION, font(Font.PLAIN, 12), CREAM_SOFT, band, 184);

        // Short rule + publisher byline, anchored to the bottom of the spine.
        g.setColor(CREAM_SOFT);
        g.fillRect((band - 44) / 2, h - 62, 44, 1);
        centered(g, PUBLISHER, font(Font.PLAIN, 11), CREAM_SOFT, band, h - 42);

        g.dispose();
        return img;
    }

    // -------------------------------------------------------------------------
    // macOS: DMG volume background. Navy header band, cream body, and a navy
    // arrow in the gap between the .app icon and the Applications alias.
    // -------------------------------------------------------------------------
    private static BufferedImage dmgBackground(BufferedImage icon) {
        int w = 520, h = 340, headerH = 62;
        BufferedImage img = newImage(w, h, CREAM);
        Graphics2D g = graphics(img);

        g.setColor(NAVY);
        g.fillRect(0, 0, w, headerH);

        drawIcon(g, icon, 20, 13, 36);

        g.setFont(font(Font.BOLD, 16));
        g.setColor(CREAM);
        g.drawString(PRODUCT + " Community", 68, 33);
        g.setFont(font(Font.PLAIN, 11));
        g.setColor(CREAM_SOFT);
        g.drawString(PUBLISHER, 68, 49);

        // Install-gesture arrow, centred in the clear gap between the two 128px
        // Finder icons (which occupy x 56..184 and x 326..454).
        g.setColor(NAVY);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int ay = 130, x0 = 215, x1 = 295;
        g.drawLine(x0, ay, x1, ay);
        g.drawLine(x1 - 11, ay - 8, x1, ay);
        g.drawLine(x1 - 11, ay + 8, x1, ay);

        centered(g, "Drag " + PRODUCT + " to Applications to install",
                font(Font.PLAIN, 12), NAVY, w, 252);

        g.dispose();
        return img;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private static BufferedImage newImage(int w, int h, Color background) {
        // TYPE_INT_RGB: the BMP writer rejects images with an alpha channel, and
        // an opaque background is what both WiX and Finder expect anyway.
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(background);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private static Graphics2D graphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setComposite(AlphaComposite.SrcOver);
        return g;
    }

    private static Font font(int style, int size) {
        return new Font(Font.SANS_SERIF, style, size);
    }

    private static void drawIcon(Graphics2D g, BufferedImage icon, int x, int y, int size) {
        g.drawImage(icon, x, y, size, size, null);
    }

    /** Draws {@code text} horizontally centred inside a region of {@code width}. */
    private static void centered(Graphics2D g, String text, Font font, Color color, int width, int baselineY) {
        g.setFont(font);
        g.setColor(color);
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (width - textWidth) / 2, baselineY);
    }

    private static void write(BufferedImage img, String format, Path target) throws Exception {
        Files.createDirectories(target.getParent());
        File file = target.toFile();
        if (!ImageIO.write(img, format, file)) {
            throw new IllegalStateException("No ImageIO writer for format: " + format);
        }
        System.out.printf("wrote %s (%dx%d)%n", target, img.getWidth(), img.getHeight());
    }

    private GenerateInstallerBranding() {
    }
}
