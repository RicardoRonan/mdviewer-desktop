import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Draws a multi-resolution Windows .ico (PNG-encoded entries) without extra tooling:
 *   java packaging/make-ico.java packaging/icon.ico
 *
 * jpackage and Inno Setup both want a real .ico; PNG entries are valid for every size
 * on Vista and later, which is all MDViewer targets.
 */
public class make_ico {

    private static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};

    public static void main(String[] args) throws Exception {
        File output = new File(args.length > 0 ? args[0] : "packaging/icon.ico");
        if (output.getParentFile() != null) output.getParentFile().mkdirs();

        List<byte[]> pngs = new ArrayList<>();
        for (int size : SIZES) pngs.add(renderPng(size));

        writeIco(output, pngs);
        System.out.println("wrote " + output.getAbsolutePath() + " (" + pngs.size() + " images)");
    }

    /** Draw the launcher mark at the given square size and return PNG bytes. */
    private static byte[] renderPng(int size) throws Exception {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double s = size / 512.0; // scale factor from the 512-unit design grid

        g.setColor(new Color(0x000000));
        g.fill(new RoundRectangle2D.Double(16 * s, 16 * s, (512 - 32) * s, (512 - 32) * s, 112 * s, 112 * s));

        // Page sheet
        g.setColor(new Color(0xFFFFFF));
        g.fill(new RoundRectangle2D.Double(126 * s, 96 * s, 260 * s, 320 * s, 28 * s, 28 * s));

        // Folded corner
        g.setColor(new Color(0xEFEFEF));
        g.fillPolygon(
                new int[]{(int) (330 * s), (int) (386 * s), (int) (386 * s)},
                new int[]{(int) (96 * s), (int) (152 * s), (int) (96 * s)}, 3);

        // "M" mark
        g.setColor(new Color(0x000000));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) Math.max(1, 196 * s)));
        g.drawString("M", (float) (176 * s), (float) (316 * s));

        // Highlighter stroke
        g.setColor(new Color(0xFFE066));
        g.fill(new RoundRectangle2D.Double(150 * s, 344 * s, 212 * s, 40 * s, 12 * s, 12 * s));

        g.setStroke(new BasicStroke((float) Math.max(1, 16 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x000000));
        g.draw(new RoundRectangle2D.Double(126 * s, 96 * s, 260 * s, 320 * s, 28 * s, 28 * s));
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** Assemble a 6-byte header, directory entries, then concatenated PNG blobs. */
    private static void writeIco(File file, List<byte[]> pngs) throws Exception {
        int count = pngs.size();
        ByteBuffer dir = ByteBuffer.allocate(6 + count * 16).order(ByteOrder.LITTLE_ENDIAN);
        dir.putShort((short) 0);            // reserved
        dir.putShort((short) 1);            // type: icon
        dir.putShort((short) count);        // image count

        int offset = 6 + count * 16;
        for (int i = 0; i < count; i++) {
            byte[] png = pngs.get(i);
            int size = png.length;
            int dim = SIZES[i];
            dir.put((byte) (dim >= 256 ? 0 : dim));   // width (0 means 256)
            dir.put((byte) (dim >= 256 ? 0 : dim));   // height
            dir.put((byte) 0);          // palette colors
            dir.put((byte) 0);          // reserved
            dir.putShort((short) 1);    // color planes
            dir.putShort((short) 32);   // bits per pixel
            dir.putInt(size);           // data size
            dir.putInt(offset);         // data offset
            offset += size;
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(dir.array());
            for (byte[] png : pngs) fos.write(png);
        }
    }
}
