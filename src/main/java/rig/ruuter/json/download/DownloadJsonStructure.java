package rig.ruuter.json.download;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JsonPropertyOrder({"filename", "hash"})
public class DownloadJsonStructure {
    String filename;
    String hash;
}
