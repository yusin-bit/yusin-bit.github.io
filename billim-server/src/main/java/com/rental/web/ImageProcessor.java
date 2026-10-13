package main.java.com.rental.web;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * 업로드된 물품 사진을 검사하고 JPEG 로 다시 저장한다.
 * - 파일 앞부분(매직 바이트)으로 진짜 JPEG/PNG 인지 확인 → 이미지로 위장한 파일 차단
 * - 픽셀 수를 먼저 읽어 너무 큰 이미지(압축 폭탄)는 풀기 전에 거절
 * - 새로 그려서 저장하므로 촬영 위치(GPS) 같은 메타데이터가 남지 않는다
 */
final class ImageProcessor {

    static final int MAX_UPLOAD_BYTES = 512 * 1024;
    private static final int MAX_SIDE = 1000;
    private static final int MAX_SOURCE_SIDE = 6000;
    private static final long MAX_SOURCE_PIXELS = 24_000_000L;

    private ImageProcessor() { }

    /** 무료 서버 메모리(512MB)를 아끼려고 한 번에 한 장씩만 처리한다 */
    static synchronized byte[] toJpeg(byte[] input) throws IOException {
        if (input == null || input.length == 0) throw new IllegalArgumentException("사진 파일이 비어 있습니다.");
        if (input.length > MAX_UPLOAD_BYTES) throw new IllegalArgumentException("사진은 512KB 이하만 올릴 수 있습니다.");
        if (!isJpeg(input) && !isPng(input)) throw new IllegalArgumentException("JPG 또는 PNG 사진만 올릴 수 있습니다.");

        BufferedImage source;
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) throw new IllegalArgumentException("사진 파일을 읽을 수 없습니다.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true); // 메타데이터는 읽지 않음
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_SOURCE_SIDE || height > MAX_SOURCE_SIDE
                        || (long) width * height > MAX_SOURCE_PIXELS)
                    throw new IllegalArgumentException("사진 크기가 너무 큽니다. (가로·세로 6000픽셀 이하)");
                source = reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException | RuntimeException broken) {
            throw new IllegalArgumentException("사진 파일이 손상되었거나 읽을 수 없습니다.");
        }

        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(source.getWidth(), source.getHeight()));
        int w = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setColor(Color.WHITE); // 투명한 PNG 는 흰 배경 위에
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.82f);
            writer.write(null, new IIOImage(target, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static boolean isJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] b) {
        return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }
}
