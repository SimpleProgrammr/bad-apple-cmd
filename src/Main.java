import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class Main {
    private static final char[] chars = {
            ' ', '.', ',', ':', '-', '~', '=', '+',
            '*', '#', '%', '@'
    };
    public static int scale = 5;
    public static void main(String[] args) {

        if(args.length > 0)
            scale = Integer.parseInt(args[0]);


        String dir = ".\\bad-apple\\image_sequence";
        try {
            ArrayList<String> frames = new ArrayList<>(GetFilesInDirList(dir));
            frames.sort(null);

            int framesCount = frames.size();


            List<BufferedImage> readyToConversionImages = new ArrayList<>(Collections.nCopies(frames.size(), null));
            AtomicInteger readyCount = new AtomicInteger();

            IntStream.range(0, frames.size()).parallel().forEach(i -> {
                String f = frames.get(i);
                BufferedImage image = null;

                try {
                    image = ImageIO.read(new File(dir + "\\" + f));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                int w = image.getWidth();
                int h = image.getHeight();


                int width = w * scale / 100;
                int height = h * scale / 100;

                BufferedImage resized = getResizedBufferedImage(image, width, height);

                readyToConversionImages.set(i, resized);  // <-- zachowanie kolejności

                int count = readyCount.incrementAndGet();
                System.out.print("\rReady images:  " + count + " / " + frames.size() + " ...");
            });

            if(scale>=10)
                playMusic(".\\bad-apple\\bad_apple.wav");

            long timer_stop, timer_start;
            AtomicInteger counter = new AtomicInteger();
            for (var image : readyToConversionImages) {
                timer_start = System.nanoTime();


                int width = image.getWidth();
                int height = image.getHeight();

                char[][] outCharsFrame = new char[height][width];

                IntStream.range(0, width).parallel().forEach(x -> {
                    IntStream.range(0, height).parallel().forEach(y -> {
                        var pixel = image.getRGB(x, y);

                        outCharsFrame[y][x] = getPixelToChar(pixel);
                    });
                });
                String[] outLinesFrame = new String[height];
                IntStream.range(0, height).parallel().forEach(y -> {

                    for (char c : outCharsFrame[y]) {
                        outLinesFrame[y] += c;
                    }
                });
                clearConsole();
                for (var line : outLinesFrame) {
                    System.out.println(line.substring(4, line.length() - 1));
                }



                timer_stop = System.nanoTime();
                long time_lap = (32 - (timer_stop - timer_start) / 1_000_000);
                Thread.sleep(Math.max(0, time_lap ), (int) (timer_stop%1_000_000));
                System.out.println(counter.incrementAndGet());

            }

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return;
    }

    private static char getPixelToChar(int pixel)
    {
        var R = pixel >> 16 & 0xFF;
        var G = pixel >> 8 & 0xFF;
        var B = pixel & 0xFF;
        var avgColor = (R + G + B) / 3;

        int opacitySpace = 255/chars.length;
        int index = Math.min(chars.length-1, Math.max(0,avgColor/opacitySpace));
        return chars[index];
    }

    private static void playMusic(String musicPath)
    {
        new Thread(() -> {
            try {
                File muzyka = new File(musicPath); // Plik musi być w katalogu projektu
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(muzyka);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.loop(Clip.LOOP_CONTINUOUSLY); // Pętla w tle
                clip.start();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static List<String> GetFilesInDirList(String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet())
                    .stream().toList();
        }
    }

    public static BufferedImage getResizedBufferedImage(BufferedImage originalImage, int newWidth, int newHeight) {
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();

        // Wysoka jakość skalowania
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Rysowanie zmniejszonego obrazu
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resizedImage;
    }

    public static void clearConsole() {
        try {
            String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}