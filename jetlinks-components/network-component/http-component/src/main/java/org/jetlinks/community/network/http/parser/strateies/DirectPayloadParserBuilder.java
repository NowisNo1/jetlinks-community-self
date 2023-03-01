package org.jetlinks.community.network.http.parser.strateies;

import lombok.SneakyThrows;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.network.http.parser.DirectRecordParser;
import org.jetlinks.community.network.http.parser.PayloadParser;
import org.jetlinks.community.network.http.parser.PayloadParserBuilderStrategy;
import org.jetlinks.community.network.http.parser.PayloadParserType;

public class DirectPayloadParserBuilder implements PayloadParserBuilderStrategy {

    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DIRECT;
    }

    @Override
    @SneakyThrows
    public PayloadParser build(ValueObject config) {
        return new DirectRecordParser();
    }
}
