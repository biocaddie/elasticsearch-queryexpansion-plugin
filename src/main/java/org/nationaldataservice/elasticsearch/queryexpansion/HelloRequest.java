package org.nationaldataservice.elasticsearch.queryexpansion;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentHelper;

public class HelloRequest extends ActionRequest {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public void setRestContent(BytesReference restContent) {
        // Let's try to find the name from the body
        @SuppressWarnings("deprecation")
		Map<String, Object> map = XContentHelper.convertToMap(restContent, false).v2();
        if (map.containsKey("name")) {
            name = (String) map.get("name");
        }
    }

    public String getName() {
        return name;
    }
    
    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (name == null) {
            validationException = new ActionRequestValidationException();
            validationException.addValidationError("You must provide a name");
            return validationException;
        }
        return null;
    }
    
    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        name = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalString(name);
    }
}
