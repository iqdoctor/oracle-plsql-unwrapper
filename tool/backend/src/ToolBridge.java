import com.github.sftwnd.oracle.unwrap.spi.FormatUnwrapper;
import com.github.sftwnd.oracle.unwrap.spi.WrappedUnit;
import com.github.sftwnd.oracle.unwrap.v2.V2FormatUnwrapper;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

/** Stdin/stdout-only adapter for the public V2 provider. */
public final class ToolBridge {
  private static String b64(String s) { return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
  private static void result(String status, String type, String name, String source) {
    System.out.println(status + "\t" + b64(type) + "\t" + b64(name) + "\t" + b64(source));
  }
  private static String decode(byte[] bytes, String charset) throws Exception {
    CharsetDecoder d = Charset.forName(charset).newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
    return d.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
  }
  public static void main(String[] args) {
    try {
      String charset = args.length > 0 ? args[0] : "UTF-8";
      ByteArrayOutputStream input = new ByteArrayOutputStream();
      byte[] chunk = new byte[8192]; int count;
      while ((count = System.in.read(chunk)) >= 0) input.write(chunk, 0, count);
      String text = decode(input.toByteArray(), charset);
      // Header-only V1 recognition, including dictionary-style headers; never parse or restore V1.
      java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?im)^[ \\t]*(?:CREATE\\s+(?:OR\\s+REPLACE\\s+)?|)(PACKAGE(?:\\s+BODY)?|PROCEDURE|FUNCTION|TYPE(?:\\s+BODY)?)\\s+([^\\r\\n]+?)\\s+wrapped[ \\t]*$");
      java.util.regex.Matcher m = p.matcher(text);
      int v1 = 0;
      while (m.find()) {
        int end=m.end(); String tail=text.substring(end).replaceFirst("^(?:\\r?\\n|\\r)", "");
        String[] lines=tail.split("\\r?\\n|\\r", 4);
        if (lines.length >= 2 && lines[0].trim().equals("0") && lines[1].trim().equalsIgnoreCase("abcd")) v1++;
      }
      if (v1 > 0) { result("V1","","",""); return; }
      FormatUnwrapper provider = new V2FormatUnwrapper();
      List<WrappedUnit> units = new ArrayList<>();
      int cursor=0;
      try {
        while (cursor < text.length()) {
          int at=provider.find(text,cursor); if(at<0) break;
          WrappedUnit unit=provider.read(text,at);
          if (unit.end() <= at || unit.end() > text.length()) { result("INVALID","","",""); return; }
          units.add(unit); cursor=unit.end();
          if (units.size()>1) break;
        }
      } catch (RuntimeException ex) { result("INVALID","","",""); return; }
      if (units.isEmpty()) { result("NOT_WRAPPED","","",""); return; }
      if (units.size()>1) { result("MULTI","","",""); return; }
      WrappedUnit unit=units.get(0);
      String restored=unit.unwrap();
      result("OK", unit.unitType(), unit.unitName(), restored);
    } catch (OutOfMemoryError e) { result("ERROR","","",""); }
      catch (Throwable e) { result("ERROR","","",""); }
  }
}
