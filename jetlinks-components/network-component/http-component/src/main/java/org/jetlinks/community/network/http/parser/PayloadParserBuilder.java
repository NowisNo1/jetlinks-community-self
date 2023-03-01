package org.jetlinks.community.network.http.parser;

import org.jetlinks.community.ValueObject;

public interface PayloadParserBuilder {

    PayloadParser build(PayloadParserType type, ValueObject configuration);

}
