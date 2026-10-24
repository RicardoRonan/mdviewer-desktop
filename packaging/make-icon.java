import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the launcher icon without extra tooling: java packaging/make-icon.java packaging/icon.png
 */
public class make_icon {

    public static void main(String[] args) throws Exception {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(0x000000));
        g.fill(new RoundRectangle2D.Double(16, 16, size - 32, size - 32, 112, 112));

        // Page sheet
        g.setColor(new Color(0xFFFFFF));
        g.fill(new RoundRectangle2D.Double(126, 96, 260, 320, 28, 28));

        // Folded corner
        g.setColor(new Color(0xEFEFEF));
        g.fillPolygon(new int[]{330, 386, 386}, new int[]{96, 152, 96}, 3);

        // "M" mark
        g.setColor(new Color(0x000000));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 196));
        g.drawString("M", 176, 316);

        // Highlighter stroke
        g.setColor(new Color(0xFFE066));
        g.fill(new RoundRectangle2D.Double(150, 344, 212, 40, 12, 12));

        g.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x000000));
        g.draw(new RoundRectangle2D.Double(126, 96, 260, 320, 28, 28));
        g.dispose();

        File output = new File(args.length > 0 ? args[0] : "packaging/icon.png");
        output.getParentFile().mkdirs();
        ImageIO.write(image, "png", output);
        System.out.println("wrote " + output.getAbsolutePath());
    }
}
