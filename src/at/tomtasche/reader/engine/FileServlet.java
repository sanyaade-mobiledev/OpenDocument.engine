package at.tomtasche.reader.engine;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import at.andiwand.commons.lwxml.writer.LWXMLStreamWriter;
import at.andiwand.commons.lwxml.writer.LWXMLWriter;
import at.andiwand.odf2html.odf.IllegalMimeTypeException;
import at.andiwand.odf2html.odf.OpenDocument;
import at.andiwand.odf2html.odf.OpenDocumentFile;
import at.andiwand.odf2html.odf.OpenDocumentSpreadsheet;
import at.andiwand.odf2html.odf.OpenDocumentText;
import at.andiwand.odf2html.odf.TemporaryOpenDocumentFile;
import at.andiwand.odf2html.translator.document.SpreadsheetTranslator;
import at.andiwand.odf2html.translator.document.TextTranslator;
import at.tomtasche.reader.engine.model.ClientFile;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

@SuppressWarnings("serial")
public class FileServlet extends DriveServlet {

	/**
	 * Given a {@code file_id} URI parameter, return a JSON representation of
	 * the given file.
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Drive service = getDriveService(req, resp);
		String fileId = req.getParameter("file_id");

		if (fileId == null) {
			sendError(resp, 400,
					"The `file_id` URI parameter must be specified.");
			return;
		}

		File file = null;
		try {
			file = service.files().get(fileId).execute();
		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 401) {
				// The user has revoked our token or it is otherwise bad.
				// Delete the local copy so that their next page load will
				// recover.
				deleteCredential(req, resp);
				sendError(resp, 401, "Unauthorized");
				return;
			}
		}

		if (file != null) {
			String password = null;
			CharArrayWriter writer = new CharArrayWriter();
			try {
				OpenDocumentFile documentFile = new TemporaryOpenDocumentFile(
						getFileContent(service, file));
				String mimeType = documentFile.getMimetype();
				if (!OpenDocument.checkMimetype(mimeType)) {
					throw new IllegalMimeTypeException();
				}

				if (documentFile.isEncrypted() && password == null) {
					throw new RuntimeException("Password required.");
				} else if (password != null) {
					documentFile.setPassword(password);
				}

				OpenDocument document = documentFile.getAsOpenDocument();
				if (document instanceof OpenDocumentText) {
					LWXMLWriter out = new LWXMLStreamWriter(writer);
					try {
						TextTranslator translator = new TextTranslator();
						translator.translate(document, out);
					} finally {
						out.close();
						writer.close();
					}
				} else if (document instanceof OpenDocumentSpreadsheet) {
					SpreadsheetTranslator translator = new SpreadsheetTranslator();
					LWXMLWriter out = new LWXMLStreamWriter(writer);
					try {
						translator.translate(document, out);
					} finally {
						out.close();
						writer.close();
					}
				} else {
					throw new IllegalMimeTypeException(
							"I don't know what it is, but I can't stop parsing it");
				}
			} catch (Exception e) {
				e.printStackTrace();

				Logger.getAnonymousLogger().log(Level.WARNING, e.toString());
			}

			String result = writer.toString();
			Logger.getAnonymousLogger().log(Level.WARNING, result);

			resp.setContentType(JSON_MIMETYPE);
			resp.getWriter().print(new ClientFile(file, result).toJson());
		} else {
			sendError(resp, 404, "File not found");
		}
	}
}