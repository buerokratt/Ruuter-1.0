package rig.ruuter.constant;

public class Constant {

    /**
     * Configuration json constants
     */
    public static final String ACTION_TYPE = "action_type";
    public static final String DESTINATION = "destination";
    public static final String VERIFY = "verify";
    public static final String ENDPOINT = "endpoint";
    public static final String SKIP = "skip";
    public static final String ENDPOINT_PARAMETERS = "endpoint_parameters";
    public static final String POST_BODY_PARAMETERS = "post_body_parameters";
    public static final String PARAMETERS = "parameters";
    public static final String REQUEST = "request";
    public static final String REQUEST_HEADER = "request_header";
    public static final String MULTI_REQUEST = "multi_request";
    public static final String MULTI_REQUEST_FIELD = "field";
    public static final String MULTI_REQUEST_COLLECTION = "collection";
    public static final String POST_BODY_STRUCT = "post_body_struct";
    public static final String METHOD = "method";
    public static final String POST = "post";
    public static final String PUT = "put";
    public static final String GET = "get";
    public static final String RESPONSE = "response";
    public static final String RESPONSE_HEADER = "response_header";
    public static final String DEFAULT_CUSTOM_JWT_NAME = "CUSTOMJWT";
    public static final String RESP_OK = "ok";
    public static final String RESP_NOK = "nok";
    public static final String ON_ERROR = "on_error";
    public static final String STOP = "stop";
    public static final String DATA = "data";
    public static final String OUTPUT_FROM = "output_from";
    public static final String ERROR = "error";
    public static final String COOKIES = "cookies";
    public static final String CONNECT_TIMEOUT_MS = "connect_timeout_ms";
    public static final String DOWNLOAD_EXTRACT_PATH = "extract_from_json";
    public static final String DOWNLOAD_DATA = "data";
    public static final String DOWNLOAD_FILENAME = "filename";
    public static final String REQUESTED_METHOD_TYPE = "requested_method_type";
    public static final String ALLOW_INCOMING_HEADERS_FORWARDING = "allow_incoming_headers_forwarding";
    public static final String PASSTHROUGH_RESPONSE_HEADERS = "passthrough_response_headers";

    public static final String RUUTER_BASE_RESPONSE_STRUCTURE = "{\"data\": {}, \"error\":null}";
    public static final String INCOMING_BODY = "{$incoming_request_body}";

    /**
     * General purpose
     */
    public static final String LINE_BREAK = "\n";
    public static final String DATE_TIME_FORMAT = "dd.MM.yyy HH:mm:ss.S";

    /**
     * Request UID related.
     * TODO: Purge the manual REQUEST_UID and REQ_GUID and only use sleuth traceId and spanId instead.
     */
    //header name for http header that contains request UID
    @Deprecated
    public static final String REQUEST_UID = "REQUEST_UID";
    //default LogHandler implementation stores request UID in MDC using this string as keyword
    @Deprecated
    public static final String REQ_GUID = "REQ_GUID";

    private Constant() {
    }
}
