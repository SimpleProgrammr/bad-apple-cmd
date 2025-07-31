import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrepareClass {
    public static void main(String[] args) {
        prepareFramesNames("E:\\Szkola\\Informatyka\\Java\\bad-apple-cmd\\bad-apple\\image_sequence");
    }

    public static void prepareFramesNames(String path)
    {
        try {
            var files = GetFilesInDirList(path);

            for(var file : files)
            {
                String frameNum = file.replace("bad_apple_","").replace(".png","");
                if(frameNum.length()==3)
                {
                    String fullPath = path + "\\" + file;
                    Files.move(Paths.get(fullPath), Path.of(path + "\\bad_apple_0" + frameNum + ".png"));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



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
}
