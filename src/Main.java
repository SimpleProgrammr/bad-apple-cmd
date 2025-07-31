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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class Main {
    private static final String[] CHARS = {
            "  ", "..", ",,", "::", "--", "~~", "==", "++",
            "**", "##", "%%", "@@"
    };
    public static int SCALE = 5;
    private static boolean REVERSE = false;
    private static final String VERSION = "0.1.07.25";
    private static String FRAMES_DIRECTORY = ".\\bad-apple\\image_sequence";
    private static String FRAME_PATH;
    private static String MUSIC_FILE_PATH = ".\\bad-apple\\bad_apple.wav";
    private static boolean PLAY_MUSIC = false;
    private static boolean ONE_FRAME_MODE = false;
    private static int FRAME_TIME = 32;


    public static void main(String[] args) {
        clearConsole();
        if (CLI(args)) return;


        try {
            List<BufferedImage> readyToConversionImages = getReadyimages();


            if (PLAY_MUSIC)
                playMusic(MUSIC_FILE_PATH);

            long timer_stop, timer_start;

            for (var image : readyToConversionImages) {
                timer_start = System.nanoTime();


                convertAndPrintFrame(image);


                timer_stop = System.nanoTime();
                long time_lap = (FRAME_TIME - (timer_stop - timer_start) / 1_000_000);
                Thread.sleep(Math.max(0, time_lap), (int) (timer_stop % 1_000_000));

            }

        } catch (Exception e) {
            System.out.println("Error in toChar conversion!");
            System.out.println("Error: " + e.getMessage());
        }
    }

    /// Returns true on Error
    ///
    /// Handles CLI variables
    private static boolean CLI(String[] args) {
        for (var arg : args) {
            if (arg.equals("-h") || arg.equals("-help")) {
                System.out.println(
                        """
                                Help for: bad-apple-cmd
                                -h\t--help\t\t|\tPrints help
                                -v\t--version\t|\tPrints version
                                -s=\t--scale=<val>\t|\tSets the scale [Default: 5] [WARNING] High RAM consumption. -Xmx might be needed
                                -r\t--reverse\t|\tInverts black and white [Default: false]
                                -i=\t-input=\t\t|\tSets different directory for input frames files [Default: .\\bad-apple\\image_sequence]
                                -m\t-music\t\t|\tTurns on the music [Default: false] [WARNING: Supports only AIFC, AIFF, AU, SND, and WAV formats]
                                -mp=\t-mpath=\t\t|\tSets music file path [Default: .\\bad-apple\\bad_apple.wav]
                                -f=\t-fps=\t\t|\tSets framerate target[Default: 30 fps] [WARNING: More may not my possible. CMD and CPU limitations]
                                   \t     \t\t \tTo get higher fps limit decrease reduce scale
                        """);
                return true;
            } else if (arg.equals("-v") || arg.equals("-version")) {
                System.out.println("Program version is: " + VERSION);
                return true;
            } else if (arg.contains("-s=") || arg.contains("-scale=")) {
                try {
                    if (arg.contains("-s="))
                        SCALE = Integer.parseInt(arg.replace("-s=", ""));
                    else
                        SCALE = Integer.parseInt(arg.replace("-scale=", ""));
                }
                catch (NumberFormatException e) {
                    System.out.println("Invalid scale parameter: " + arg);
                    return true;
                }
                System.out.println("Scale set to: " + SCALE);
            } else if (arg.equals("-r") || arg.equals("-reverse")) {
                REVERSE = true;

                System.out.println("Reverse mode enabled");
            } else if (arg.contains("-i=") || arg.contains("-input=")) {
                try {
                    String arg_path;
                    if (arg.contains("-i="))
                        arg_path = arg.replace("-i=", "");
                    else
                        arg_path = arg.replace("-input=", "");

                    Path path = Path.of(arg_path);
                    if (Files.isDirectory(path)) {
                        FRAMES_DIRECTORY = arg_path;
                        System.out.println("Directory found: " + arg_path);
                    } else if (Files.isRegularFile(path)) {
                        ONE_FRAME_MODE = true;
                        FRAME_PATH = arg_path;
                        System.out.println("File found: " + arg_path);
                    } else {
                        System.out.println("File not found: " + arg_path);
                        return true;
                    }
                } catch (Exception e) {
                    System.out.println("File path invalid: " + arg);
                    System.out.println(e.getMessage());
                    return true;
                }
            } else if (arg.equals("-m") || arg.equals("-music")) {

            } else if (arg.contains("-mp=") || arg.contains("-mpath=")) {

                if (arg.contains("-mp="))
                    MUSIC_FILE_PATH = arg.replace("-mp=", "");
                else
                    MUSIC_FILE_PATH = arg.replace("-mpath=", "");

                if(!Files.isRegularFile(Path.of(MUSIC_FILE_PATH))) {
                    System.out.println("Music file path invalid: " + MUSIC_FILE_PATH);
                    return true;
                }

                System.out.println("Music file found: " + MUSIC_FILE_PATH);
                PLAY_MUSIC = true;
                System.out.println("Play music mode enabled");
            }
            else if (arg.contains("-f=") || arg.contains("-fps=")) {
                try {
                    if (arg.contains("-f="))
                        FRAME_TIME = 1000 / Integer.parseInt(arg.replace("-f=", ""));
                    else
                        FRAME_TIME = 1000 / Integer.parseInt(arg.replace("-fps=", ""));
                }
                catch (NumberFormatException e) {
                    System.out.println("Invalid fps: " + arg);
                }
                System.out.println("FPS set to: " + 1000/FRAME_TIME);
                FRAME_TIME = (FRAME_TIME -1);

            }
        }
        return false;
    }

    private static void convertAndPrintFrame(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        String[][] outCharsFrame = new String[height][width];

        IntStream.range(0, width).parallel().forEach(x ->
                IntStream.range(0, height).parallel().forEach(y -> {
            var pixel = image.getRGB(x, y);

            outCharsFrame[y][x] = getPixelToChar(pixel);
        }));
        String[] outLinesFrame = new String[height];
        IntStream.range(0, height).parallel().forEach(y -> {

            for (String c : outCharsFrame[y]) {
                outLinesFrame[y] += c;
            }
        });
        clearConsole();
        for (var line : outLinesFrame) {
            System.out.println(line.substring(4, line.length() - 1));
        }
    }

    private static List<BufferedImage> getReadyimages() throws IOException {
        ArrayList<String> frames = new ArrayList<>();
        if (ONE_FRAME_MODE) {
            frames.add(FRAME_PATH);
        } else {
            frames.addAll(GetFilesInDirList(FRAMES_DIRECTORY));
            frames.sort(null);
        }

        int framesCount = frames.size();
        List<BufferedImage> readyToConversionImages = new ArrayList<>(Collections.nCopies(framesCount, null));
        AtomicInteger readyCount = new AtomicInteger();

        if (SCALE == 100) {
            IntStream.range(0, framesCount).parallel().forEach(i -> {
                String f = frames.get(i);
                BufferedImage image;
                try {
                    image = ImageIO.read(new File(FRAMES_DIRECTORY + "\\" + f));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                readyToConversionImages.set(i, image);
                System.out.print("\rReady images:  " + readyCount.incrementAndGet() + " / " + framesCount + " ...");
            });
        } else {
            IntStream.range(0, framesCount).parallel().forEach(i -> {
                String f = frames.get(i);
                BufferedImage image;

                try {
                    if(ONE_FRAME_MODE)
                        image = ImageIO.read(new File(FRAME_PATH));
                    else
                        image = ImageIO.read(new File(FRAMES_DIRECTORY + "\\" + f));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                int w = image.getWidth();
                int h = image.getHeight();


                int width = w * SCALE / 100;
                int height = h * SCALE / 100;

                BufferedImage resized = getResizedBufferedImage(image, width, height);

                readyToConversionImages.set(i, resized);  // <-- zachowanie kolejności

                System.out.print("\rReady images:  " + readyCount.incrementAndGet() + " / " + framesCount + " ...");
            });
        }
        return readyToConversionImages;
    }

    private static String getPixelToChar(int pixel) {
        var R = pixel >> 16 & 0xFF;
        var G = pixel >> 8 & 0xFF;
        var B = pixel & 0xFF;
        var avgColor = (R + G + B) / 3;

        int opacitySpace = 255 / CHARS.length;
        int index = Math.min(CHARS.length - 1, Math.max(0, avgColor / opacitySpace));
        return REVERSE ? CHARS[CHARS.length - 1 - index] : CHARS[index];
    }

    private static void playMusic(String musicPath) {
        new Thread(() -> {
            try {
                File muzyka = new File( musicPath); // Plik musi być w katalogu projektu
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(muzyka);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.loop(Clip.LOOP_CONTINUOUSLY); // Pętla w tle
                clip.start();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                if(e.getCause() instanceof UnsupportedAudioFileException) {
                    System.out.println("UnsupportedAudioFileException: " + e.getCause().getMessage());
                }
                else if (e.getCause() instanceof LineUnavailableException) {
                    System.out.println("LineUnavailableException: " + e.getCause().getMessage());
                }
                else {
                    System.out.println("IOException: " + e.getCause().getMessage());
                }

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
            System.out.println("CLEAR CONSOLE ERROR!");
            System.out.println("Error: " + e.getMessage());
        }
    }

}