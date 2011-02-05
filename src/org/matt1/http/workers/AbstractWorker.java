package org.matt1.http.workers;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Vector;

import org.matt1.http.utils.headers.ContentTypeHttpHeader;
import org.matt1.http.utils.headers.DateHttpHeader;
import org.matt1.http.utils.headers.HttpHeader;
import org.matt1.http.utils.headers.ServerHttpHeader;
import org.matt1.http.utils.response.HttpStatus;
import org.matt1.utils.Logger;

/**
 * <p>
 * An abstract worker provides a basic set of basic common functionality that can be shared by many different types
 * or worker
 * </p>
 * @author Matt
 *
 */
public abstract class AbstractWorker implements Runnable, WorkerInterface {

	private final ServerHttpHeader mServerHeader = new ServerHttpHeader();
	
	@Override
	public abstract void InitialiseWorker(Socket pSocket, File pRootDirectory);

	@Override
	public abstract void InitialiseWorker(Socket pSocket, int pTimeout, File pRootDirectory);

	@Override
	public abstract void run();

	/**
	 * <p>
	 * Given the data and the socket, write the response to the client.  Will automatically provide headers for
	 * content-length, but content type should be provided by the worker as a separate header
	 * </p>
	 * @param pData Actual bytes to write
	 * @param pSocket Socket to write to
	 * @param pHeaders Any additional headers to provide
	 */
	protected void writeResponse(byte[] pData, Socket pSocket, List<HttpHeader> pHeaders, HttpStatus pStatus) {
		try {
			
			if (!pSocket.isClosed() && pSocket.isConnected()) {
			
				OutputStream outStream = pSocket.getOutputStream();				
				outStream.write(("HTTP/1.0 " + pStatus.getDescription() + System.getProperty("line.separator")).getBytes());
				
				// Do headers
				for (HttpHeader header : pHeaders) {
					outStream.write(header.getBytes());
				}
				outStream.write(new HttpHeader("Content-length", String.valueOf(pData.length)).getBytes());
				outStream.write(new DateHttpHeader().getBytes());
				outStream.write(mServerHeader.getBytes());
				outStream.write(System.getProperty("line.separator").getBytes());
				
				outStream.write(pData, 0, pData.length);
				
				outStream.close();
			} else {
				Logger.debug("Socket was closed or disconnected before we could send response!");
			}			
		} catch (SocketException se) {
			Logger.debug("Got socket exception: " + se.getMessage());
		} catch (IOException e) {
			Logger.error("IOException when trying to write response!");
		} 	
	}

	/**
	 * <p>
	 * Writes a simple text HTML response out to the socket
	 * </p>
	 * @param pResponse Stream of bytes to write
	 */
	protected void writeResponse(String pResponse, Socket pSocket, HttpStatus pStatus) {
		List<HttpHeader> headers = new Vector<HttpHeader>();
		headers.add(new ContentTypeHttpHeader("text/html"));
		writeResponse(pResponse.getBytes(), pSocket, headers, HttpStatus.HTTP200);
	}
	
	/**
	 * <p>
	 * Send a simple response message (e.g. HTTP 500 - server error")
	 * </p>
	 * @param pSocket
	 * @param pStatus
	 */
	protected void writeStatus(Socket pSocket, HttpStatus pStatus) {
		writeResponse(pStatus.getDescription(), pSocket, pStatus);
	}

}