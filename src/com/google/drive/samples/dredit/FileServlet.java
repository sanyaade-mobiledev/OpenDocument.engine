/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.drive.samples.dredit;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.drive.samples.dredit.model.ClientFile;

/**
 * Servlet providing a small API for the DrEdit JavaScript client to use in
 * manipulating files. Each operation (GET, POST, PUT) issues requests to the
 * Google Drive API.
 * 
 * @author vicfryzel@google.com (Vic Fryzel)
 */
@SuppressWarnings("serial")
public class FileServlet extends DrEditServlet {

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
			StringWriter stringWriter = new StringWriter();
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
					CharArrayWriter writer = new CharArrayWriter();
					LWXMLWriter out = new LWXMLStreamWriter(writer);
					try {
						TextTranslator translator = new TextTranslator();
						translator.translate(document, out);

						writer.writeTo(stringWriter);
					} finally {
						out.close();
						writer.close();
					}
				} else if (document instanceof OpenDocumentSpreadsheet) {
					SpreadsheetTranslator translator = new SpreadsheetTranslator();
					CharArrayWriter writer = new CharArrayWriter();
					LWXMLWriter out = new LWXMLStreamWriter(writer);
					try {
						translator.translate(document, out);

						writer.writeTo(stringWriter);
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

			String result = stringWriter.toString();

			Logger.getAnonymousLogger().log(Level.WARNING, result);

			resp.setContentType(JSON_MIMETYPE);
			resp.getWriter().print(new ClientFile(file, result).toJson());
		} else {
			sendError(resp, 404, "File not found");
		}
	}

	private InputStream getFileContent(Drive service, File file)
			throws IOException {
		GenericUrl url = new GenericUrl(file.getDownloadUrl());
		HttpResponse response = service.getRequestFactory()
				.buildGetRequest(url).execute();
		return response.getContent();
	}

	/**
	 * Build and return a Drive service object based on given request
	 * parameters.
	 * 
	 * @param req
	 *            Request to use to fetch code parameter or accessToken session
	 *            attribute.
	 * @param resp
	 *            HTTP response to use for redirecting for authorization if
	 *            needed.
	 * @return Drive service object that is ready to make requests, or null if
	 *         there was a problem.
	 */
	private Drive getDriveService(HttpServletRequest req,
			HttpServletResponse resp) {
		Credential credentials = getCredential(req, resp);

		return Drive.builder(TRANSPORT, JSON_FACTORY)
				.setHttpRequestInitializer(credentials).build();
	}
}