package org.jetlinks.community.network.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;

/**
 * @author LuoXingyue
 * @since 1.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HttpMessage implements EncodedMessage {

    private ByteBuf payload;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        ByteBufUtil.appendPrettyHexDump(builder,payload);

        return builder.toString();
    }
}
