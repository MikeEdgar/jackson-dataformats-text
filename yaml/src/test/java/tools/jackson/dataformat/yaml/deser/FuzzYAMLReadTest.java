package tools.jackson.dataformat.yaml.deser;

import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import tools.jackson.dataformat.yaml.ModuleTestBase;

/**
 * Collection of OSS-Fuzz found issues for YAML format module.
 */
public class FuzzYAMLReadTest extends ModuleTestBase
{
    private final ObjectMapper YAML_MAPPER = newObjectMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50036
    public void testUTF8Decoding50036() throws Exception
    {
        byte[] INPUT = new byte[] { 0x20, (byte) 0xCD };
        try {
            YAML_MAPPER.readTree(INPUT);
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "End-of-input after first 1 byte");
            verifyException(e, "of a UTF-8 character");
        }
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50339
    public void testTagDecoding50339() throws Exception
    {
        final String DOC = "[!!,";
        try {
            YAML_MAPPER.readTree(DOC);
            fail("Should not pass");
        } catch (StreamReadException e) {
            // 19-Aug-2022, tatu: The actual error we get is from SnakeYAML
            //    and might change. Should try matching it at all?
            // 24-Sep-2022, tatu: ... and snakeyaml-engine has different
            verifyException(e, "while scanning");
        }
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50407
    public void testNumberDecoding50407() throws Exception
    {
        // int, octal
        _testNumberDecoding50407("- !!int 0111-");
        _testNumberDecoding50407("- !!int 01 11");
        _testNumberDecoding50407("- !!int 01245zf");
        // long, octal
        _testNumberDecoding50407("- !!int 0123456789012345-");
        _testNumberDecoding50407("- !!int 01234567   890123");
        _testNumberDecoding50407("- !!int 0123456789012ab34");
        // BigInteger, octal
        _testNumberDecoding50407("-       !!int       0111                -        -");
    }

    private void _testNumberDecoding50407(String doc) {
        try {
            YAML_MAPPER.readTree(doc);
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid base-");
        }
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50407
    public void testNumberDecoding50052() throws Exception
    {
        // 17-Sep-2022, tatu: Could produce an exception but for now type
        //    tag basically ignored, returned as empty String otken
        JsonNode n = YAML_MAPPER.readTree("!!int");
        assertEquals(JsonToken.VALUE_STRING, n.asToken());
        assertEquals("", n.textValue());
    }

    // [dataformats-text#435], originally from
    //   https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=61823
    public void testNumberDecoding61823() throws Exception
    {
        try {
            YAML_MAPPER.readTree("!!int _ ");
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid number");
        }
    }
}