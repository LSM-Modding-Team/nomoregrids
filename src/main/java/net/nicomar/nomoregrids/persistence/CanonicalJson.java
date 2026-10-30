package net.nicomar.nomoregrids.persistence;

import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;

/** Stable object ordering for native capture digests; array order and numeric values are preserved. */
public final class CanonicalJson {
    private CanonicalJson(){}
    public static byte[] bytes(JsonElement element){return sorted(element).toString().getBytes(StandardCharsets.UTF_8);}
    private static JsonElement sorted(JsonElement element){if(element.isJsonObject()){var result=new JsonObject();for(var key:new TreeSet<>(element.getAsJsonObject().keySet()))result.add(key,sorted(element.getAsJsonObject().get(key)));return result;}if(element.isJsonArray()){var result=new JsonArray();element.getAsJsonArray().forEach(value->result.add(sorted(value)));return result;}return element;}
}
