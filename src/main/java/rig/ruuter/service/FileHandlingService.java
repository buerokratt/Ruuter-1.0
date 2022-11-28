package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import rig.commons.aop.Timed;
import rig.ruuter.enums.Splitter;
import rig.ruuter.json.download.DownloadJsonStructure;
import rig.ruuter.util.CustomClientResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static rig.ruuter.constant.Constant.*;
import static rig.ruuter.service.RequestBodyMappingUtils.extract;
import static rig.ruuter.util.RestUtils.noCacheHeaders;
import static rig.ruuter.util.RestUtils.response;
import static rig.ruuter.util.StrUtils.md5;
import static rig.ruuter.util.StrUtils.toJson;

@Slf4j
@Service
@Timed
public class FileHandlingService extends BaseService {

    // should end with a '/' as in /something/tmp/
    @Value("${ruuter.tmp.file.folder}")
    private String tmpFilesFolder;

    @Value("${ruuter.tmp.file.minimum.age}")
    private Integer tmpFileMinimumAge;

    /**
     * @param httpServletResponse forwarded from Controller
     * @param config      configuration for proceeding
     * @param cookies             request cookies
     * @param incomingRequestBody request body
     * @param requestParams       request parameters
     * @return a response containing file as a byte stream
     */
    public Mono<ResponseEntity> getFileAndPrepareForUpload(HttpServletResponse httpServletResponse,
                                                           JsonNode config,
                                                           Map<String, String> requestParams,
                                                           String incomingRequestBody,
                                                           List<Cookie> cookies) {
        CustomClientResponse response = fetchFile(httpServletResponse, config, requestParams, incomingRequestBody, cookies);
        try {
            byte[] data;
            String filename;
            if (config.has(DOWNLOAD_EXTRACT_PATH)) {
                JsonNode responseJson = toJson("{}");
                ((ObjectNode) responseJson).put(DOWNLOAD_DATA, toJson(response.getBody()));

                if (config.get(DOWNLOAD_EXTRACT_PATH).isTextual() && config.get(DOWNLOAD_EXTRACT_PATH).asText().isEmpty()) {
                    throw new RuntimeException("Invalid extraction path provided");
                }

                JsonNode extraction =
                        extract(responseJson, config.get(DOWNLOAD_EXTRACT_PATH).get(DOWNLOAD_DATA).asText(), Splitter.FROM_INCOMING_BODY);

                if (extraction == null || !extraction.isTextual() || extraction.asText().isEmpty())
                    throw new RuntimeException("Failure while retrieving file");

                data = Base64.getDecoder().decode(extraction.asText());
                JsonNode fileNameNode = extract(responseJson, config.get(DOWNLOAD_EXTRACT_PATH).get(DOWNLOAD_FILENAME).asText(), Splitter.FROM_INCOMING_BODY);

                if (fileNameNode == null)
                    throw new RuntimeException("Failed retrieving fail name");

                filename = fileNameNode.asText();
            } else {
                data = Base64.getDecoder().decode(response.getBody());
                filename = response.getHeaders().getContentDisposition().getFilename();
            }

            ByteArrayResource resource = new ByteArrayResource(data);

            HttpHeaders headers = noCacheHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            headers.add(HttpHeaders.CONTENT_LENGTH, resource.contentLength() + "");
            log.info("Ready to upload file.");
            return Mono.just(ResponseEntity.ok()
                    .headers(headers)
                    .body(resource));
        } catch (Exception e) {
            log.info("Couldn't upload file.", e);
            return response(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @param hash     unique hash identifying attachment
     * @param filename filename for download
     * @return file (as byte stream) with a filename of {@code filename} or if {@code filename} = null then with filename={@code hash}
     */
    public Mono<ResponseEntity> getInboxAttachment(String hash, String filename) {
        hash = hash.trim();
        filename = filename == null ? null : filename.trim();
        log.info("Looking for file {}", tmpFilesFolder + hash);
        File f = new File(tmpFilesFolder + hash);
        ByteArrayResource resource = null;
        FileInputStream fos = null;
        try {
            fos = new FileInputStream(f);
            resource = new ByteArrayResource(IOUtils.toByteArray(fos));
            fos.close();
            HttpHeaders headers = noCacheHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + (filename == null ? hash : filename));
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            headers.add(HttpHeaders.CONTENT_LENGTH, resource.contentLength() + "");

            FileUtils.deleteQuietly(f);

            return Mono.just(ResponseEntity.ok()
                    .headers(headers)
                    .body(resource));
        } catch (FileNotFoundException e) {
            log.error("File not found.", e);
        } catch (IOException e) {
            log.error("Couldn't prepare file for download.", e);
        }
        return response(HttpStatus.NOT_FOUND);
    }

    /**
     * @return a json structure with filename and location on disc
     */
    public Mono<ResponseEntity> saveFileAndGetLocation(HttpServletResponse httpServletResponse,
                                                       JsonNode config,
                                                       Map<String, String> requestParams,
                                                       String incomingRequestBody,
                                                       List<Cookie> cookies) {

        CustomClientResponse response = fetchFile(httpServletResponse, config, requestParams, incomingRequestBody, cookies);

        byte[] data;
        String filename;
        if (config.has(DOWNLOAD_EXTRACT_PATH)) {
            JsonNode responseJson = toJson("{}");
            ((ObjectNode) responseJson).put(DOWNLOAD_DATA, toJson(response.getBody()));

            if (config.get(DOWNLOAD_EXTRACT_PATH).isTextual() && config.get(DOWNLOAD_EXTRACT_PATH).asText().isEmpty()) {
                throw new RuntimeException("Invalid extraction path provided");
            }

            JsonNode extraction =
                    extract(responseJson, config.get(DOWNLOAD_EXTRACT_PATH).get(DOWNLOAD_DATA).asText(), Splitter.FROM_INCOMING_BODY);

            if (extraction == null || !extraction.isTextual() || extraction.asText().isEmpty())
                throw new RuntimeException("Failure while retrieving file");

            data = Base64.getDecoder().decode(extraction.asText());
            JsonNode fileNameNode = extract(responseJson, config.get(DOWNLOAD_EXTRACT_PATH).get(DOWNLOAD_FILENAME).asText(), Splitter.FROM_INCOMING_BODY);

            if (fileNameNode == null)
                throw new RuntimeException("Failed retrieving fail name");

            filename = fileNameNode.asText();
        } else {
            filename = response.getHeaders().getContentDisposition().getFilename();
            data = Base64.getDecoder().decode(response.getBody());
        }


        /**
         * construct a /tmp_files_folder/md5(currenttimemillis_filename).extension
         Request ID  is based on unique current millis for the request and stored
         in MDC under key name "REQ_GUID" for default LogHandler implementation
         **/
        String reqUID = MDC.get(REQ_GUID);
        String hash = md5(reqUID + "_" + filename.substring(0, filename.lastIndexOf(".")))
                + filename.substring(filename.lastIndexOf("."));
        String path = tmpFilesFolder + hash;
        ;
        File f = new File(path);
        f.setReadable(true);
        try {
            log.info("Will try to write file to {}", path);
            FileUtils.writeByteArrayToFile(f, data);
        } catch (IOException e) {
            log.error("Couldn't save file locally to path " + path, e);
            return response(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return f.exists()
                ? response(DownloadJsonStructure.builder().filename(filename).hash(hash).build())
                : response(HttpStatus.NOT_FOUND);
    }

    /**
     * Delete temporary files
     *
     * @return return OK on success
     */
    public Mono<ResponseEntity> deleteTmpFiles() {
        File tmpFolder = new File(tmpFilesFolder);
        log.info("Init deletion of temporary files older than {} minutes from {}", tmpFileMinimumAge, tmpFilesFolder);
        int count = 0;
        for (File file : Objects.requireNonNull(tmpFolder.listFiles())) {
            long diff = new Date().getTime() - file.lastModified();
            if (!file.isDirectory() && diff > TimeUnit.MINUTES.toMillis(tmpFileMinimumAge)) {
                file.delete();
                count++;
            }
        }
        log.info("Temporary files deletion complete. Deleted {} files.", count);
        return response(HttpStatus.OK);
    }


    private CustomClientResponse fetchFile(HttpServletResponse httpServletResponse, JsonNode proceedingConf, Map<String, String> requestParams,
                                           String incomingRequestBody, List<Cookie> cookies) {
        log.info("Fetching base64string.");
        CustomClientResponse base64FileResponse = retrieveTimedResponse(httpServletResponse,
                toJson("{}"),
                "key",
                proceedingConf,
                requestParams,
                incomingRequestBody,
                cookies);
        log.info("Got base64string of length {} and header {}",
                base64FileResponse.getBody().length(), base64FileResponse.getHeaders());
        return base64FileResponse;
    }
}
