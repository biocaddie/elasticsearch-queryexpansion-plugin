package org.nationaldataservice.elasticsearch.queryexpansion;

import java.io.IOException;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class QueryExpansionResponse extends ActionResponse implements ToXContent {

    private String message;

    public QueryExpansionResponse() {
    	this("");
    }
    
    public QueryExpansionResponse(String name) {
    	this.message = name;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        message = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalString(message);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.field("message", message);
    }
}