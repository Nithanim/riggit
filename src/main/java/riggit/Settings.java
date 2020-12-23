package riggit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Settings {

  public static Settings load(Path p) throws IOException {
    if (Files.exists(p)) {
      return new ObjectMapper().readValue(p.toFile(), Settings.class);
    } else {
      return null;
    }
  }

  public static void persist(Settings settings, Path settingsFile) throws IOException {
    Files.writeString(
        settingsFile, new ObjectMapper().writeValueAsString(settings), StandardCharsets.UTF_8);
  }

  String username;

  UUID uuid;
}
