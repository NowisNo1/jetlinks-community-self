package org.jetlinks.community.network.http.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.community.network.http.parser.strateies.PipePayloadParser;
import org.jetlinks.community.network.http.parser.strateies.ScriptPayloadParserBuilder;

@Getter
@AllArgsConstructor
@Dict("http-payload-parser-type")
public enum PayloadParserType implements EnumDict<String> {

    DIRECT("不处理"),

    FIXED_LENGTH("固定长度"),

    DELIMITED("分隔符"),

    /**
     * @see ScriptPayloadParserBuilder
     * @see PipePayloadParser
     */
    SCRIPT("自定义脚本")
    ;

    private String text;
    @Override
    public String getValue() {
        return name();
    }
}
