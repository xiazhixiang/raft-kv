package org.nuist.raft.rpc.myRpc;

import lombok.Getter;
import lombok.Setter;
import org.nuist.rpc.common.exception.remoting.RemotingCommmonCustomException;
import org.nuist.rpc.common.transport.body.CommonCustomBody;

import java.io.Serializable;

@Getter
@Setter
public class Response<T> implements CommonCustomBody, Serializable {

    private T result;

    public Response(T result) {
        this.result = result;
    }

    private Response(Response.Builder builder) {
        setResult((T) builder.result);
    }

    public static Response ok() {
        return new Response<>("ok");
    }

    public static Response fail() {
        return new Response<>("fail");
    }

    public static Response.Builder newBuilder() {
        return new Response.Builder();
    }


    @Override
    public String toString() {
        return "Response{" +
                "result=" + result +
                '}';
    }

    @Override
    public void checkFields() throws RemotingCommmonCustomException {

    }

    public static final class Builder {

        private Object result;

        private Builder() {
        }

        public Response.Builder result(Object val) {
            result = val;
            return this;
        }

        public Response build() {
            return new Response(this);
        }
    }
}
