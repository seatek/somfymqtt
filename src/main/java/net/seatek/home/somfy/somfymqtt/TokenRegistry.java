package net.seatek.home.somfy.somfymqtt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenRegistry {
	private static final String PREFIX = "token-";

	@Value("${token.registry.path}")
	private String dirPath = "tokens";

	private int validityInDays = 4;

	DateTimeFormatter f = DateTimeFormatter.ofPattern("'" + PREFIX + "'yyyy-MM-dd");
	@Autowired
	private TokenRetriever tokenRetriever;

	public SomfyToken provideToken() {
		try {

			Optional<SomfyToken> token = getTokenFromFile();
			if (token.isPresent())
				return token.get();
			return createAndStoreToken();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private SomfyToken createAndStoreToken() throws IOException {
		SomfyToken token;
		token = tokenRetriever.getToken();
		storeToken(token);

		return token;
	}

	private Optional<SomfyToken> getTokenFromFile() throws IOException {
		Comparator<File> byFilename = Comparator.comparing(File::getName);
		Path path = Paths.get(dirPath);
		Files.createDirectories(path);
		var newestToken = Files.list(path).map(Path::toFile).filter(File::isFile)
				.filter(f -> f.getName().startsWith(PREFIX)).sorted(byFilename).findFirst();
		if (newestToken.isPresent()) {
			File newestTokenFile = newestToken.get();
			LocalDate newestDate = f.parse(newestToken.get().getName(), LocalDate::from);
			if (newestDate.plusDays(validityInDays).isAfter(newestDate)) {
				String token = Files.readString(newestTokenFile.toPath()).trim();
				return Optional.of(new SomfyToken(token, newestDate));
			}
		}
		return Optional.empty();
	}

	private void storeToken(SomfyToken token) throws IOException {
		Path tokenFile = Paths.get(dirPath).resolve(f.format(token.getDate()));
		try (BufferedWriter writer = Files.newBufferedWriter(tokenFile, StandardOpenOption.CREATE_NEW)) {
			writer.write(token.getToken());
		}

	}

}
