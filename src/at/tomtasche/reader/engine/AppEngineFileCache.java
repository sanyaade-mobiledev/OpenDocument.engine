package at.tomtasche.reader.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.Map;

import at.andiwand.odf2html.translator.File2URITranslator;
import at.andiwand.odf2html.util.AbstractFileCache;

import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileReadChannel;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

public class AppEngineFileCache extends AbstractFileCache {

	private final FileService fileService;
	private final Map<String, AppEngineFile> cached;

	public AppEngineFileCache() {
		super(File2URITranslator.DEFAULT);

		fileService = FileServiceFactory.getFileService();
		cached = new HashMap<String, AppEngineFile>();
	}

	@Override
	public void clear() {
		for (String s : cached.keySet()) {
			deleteFile(s);
		}
	}

	@Override
	public void deleteFile(String arg0) {
		try {
			fileService.delete(cached.get(arg0));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public File getFile(String arg0) {
		if (!cached.containsKey(arg0))
			newFile(arg0);

		return new File(cached.get(arg0).getFullPath());
	}

	@Override
	public InputStream getFileInputStream(String arg0)
			throws FileNotFoundException {
		try {
			FileReadChannel readChannel = fileService.openReadChannel(
					cached.get(arg0), false);

			return Channels.newInputStream(readChannel);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public OutputStream getFileOutputStream(String arg0)
			throws FileNotFoundException {
		try {
			FileWriteChannel writeChannel = fileService.openWriteChannel(
					cached.get(arg0), true);

			return Channels.newOutputStream(writeChannel);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public boolean isFile(String arg0) {
		return cached.containsKey(arg0);
	}

	@Override
	public File newFile(String arg0) {
		try {
			AppEngineFile temp = fileService
					.createNewBlobFile("application/octet-stream");
			cached.put(arg0, temp);

			return new File(temp.getFullPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
