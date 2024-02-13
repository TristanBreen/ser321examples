/*
Simple Web Server in Java which allows you to call 
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a 
little easier is used. This is done so you see exactly how to pars the request and 
write a response back
*/

package funHttpServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

class WebServer {
  public static void main(String args[]) {
    WebServer server = new WebServer(9000);
  }

  /**
   * Main thread
   * @param port to listen on
   */
  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      while (true) {
        sock = server.accept();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          server.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Used in the "/random" endpoint
   */
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private Random random = new Random();

  /**
   * Reads in socket stream and generates a response
   * @param inStream HTTP input stream from socket
   * @return the byte encoded HTTP response
   */
  public byte[] createResponse(InputStream inStream) {

    byte[] response = null;
    BufferedReader in = null;

    try {

      // Read from socket's input stream. Must use an
      // InputStreamReader to bridge from streams to a reader
      in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

      // Get header and save the request from the GET line:
      // example GET format: GET /index.html HTTP/1.1

      String request = null;

      boolean done = false;
      while (!done) {
        String line = in.readLine();

        System.out.println("Received: " + line);

        // find end of header("\n\n")
        if (line == null || line.equals(""))
          done = true;
        // parse GET format ("GET <path> HTTP/1.1")
        else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);

          // extract the request, basically everything after the GET up to HTTP/1.1
          request = line.substring(firstSpace + 2, secondSpace);
        }

      }
      System.out.println("FINISHED PARSING HEADER\n");

      // Generate an appropriate response to the user
      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        // create output buffer
        StringBuilder builder = new StringBuilder();
        // NOTE: output from buffer is at the end

        if (request.length() == 0) {
          // shows the default directory page

          // opens the root.html file
          String page = new String(readFileInBytes(new File("www/root.html")));
          // performs a template replacement in the page
          page = page.replace("${links}", buildFileList());

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(page);

        } else if (request.equalsIgnoreCase("json")) {
          // shows the JSON of a random image and sets the header name for that image

          // pick a index from the map
          int index = random.nextInt(_images.size());

          // pull out the information
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n");
          builder.append("\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

        } else if (request.equalsIgnoreCase("random")) {
          // opens the random image page

          // open the index.html
          File file = new File("www/index.html");

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(new String(readFileInBytes(file)));

        } else if (request.contains("file/")) {
          // tries to find the specified file and shows it or shows an error

          // take the path and clean it. try to open the file
          File file = new File(request.replace("file/", ""));

          // Generate response
          if (file.exists()) { // success
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Would theoretically be a file but removed this part, you do not have to do anything with it for the assignment");
          } else { // failure
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("File not found: " + file);
          }
        } else if (request.contains("multiply?")) {
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes
          try {
            String[] parts = request.split("\\?")[1].split("&");
    
            int num1, num2;
          
            if (parts.length == 2 && parts[0].contains("=") && parts[1].contains("=")) {
                Map<String, String> query_pairs = new LinkedHashMap<>();
                for (String part : parts) {
                    String[] pair = part.split("=");
                    query_pairs.put(pair[0], pair[1]);
                }
                num1 = Integer.parseInt(query_pairs.get("num1"));
                num2 = Integer.parseInt(query_pairs.get("num2"));
            } else if (parts.length == 2) 
            {
                num1 = Integer.parseInt(parts[0]);
                num2 = Integer.parseInt(parts[1]);
            } else if (parts.length == 1) 
            {
                num1 = Integer.parseInt(parts[0]);
                num2 = 0;
            } else 
            {
                throw new IllegalArgumentException("Invalid number of parameters");
            }
    
            int result = num1 * num2;
    
            // Generate response
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Result is: " + result);
        } catch (Exception e) {
            // Handle invalid input or other exceptions
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Invalid input. Please provide one or two numbers in the URL query parameters.");
        }

          // TODO: Include error handling here with a correct error code and
          // a response that makes sense

        } else if (request.contains("time")) 
        {
          try {
            String continent = "UTC"; // Default continent is UTC
            String city = "UTC"; // Default city is UTC
    
            String queryString = request.contains("?") ? request.split("\\?")[1] : request;
            String[] parts = queryString.split("&");
    
            if (parts.length == 2 && parts[0].contains("=") && parts[1].contains("=")) 
            {
                for (String part : parts) 
                {
                    String[] pair = part.split("=");
                    if (pair[0].equals("continent")) 
                    {
                        continent = pair[1];
                    } else if (pair[0].equals("city")) 
                    {
                        city = pair[1];
                    }
                }
            } else if (parts.length == 2) 
            {
                continent = parts[0];
                city = parts[1];
            } else if (parts.length == 1) 
            {
                continent = parts[0];
                city = parts[0];
            } else 
            {
                throw new IllegalArgumentException("Invalid query parameters");
            }
    
            String timeZoneId = continent + "/" + city;
            ZoneId zoneId = ZoneId.of(timeZoneId);
            ZonedDateTime currentTime = ZonedDateTime.now(zoneId);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = currentTime.format(formatter);
    
            // Generate response
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Current Time in " + timeZoneId + ": " + formattedDateTime);
          } catch (Exception e) {
              // Handle any exceptions
              builder.append("HTTP/1.1 500 Internal Server Error\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Error retrieving current time.");
          }
        } else if (request.contains("quadratic?")) {
          // This solves the quadratic formula: ax^2 + bx + c = 0
          try {
              String[] parts = request.split("\\?")[1].split("&");
      
              double a, b, c;
      
              if (parts.length == 3 && parts[0].contains("=") && parts[1].contains("=") && parts[2].contains("=")) {
                  Map<String, String> query_pairs = new LinkedHashMap<>();
                  for (String part : parts) {
                      String[] pair = part.split("=");
                      query_pairs.put(pair[0], pair[1]);
                  }
                  a = Double.parseDouble(query_pairs.get("a"));
                  b = Double.parseDouble(query_pairs.get("b"));
                  c = Double.parseDouble(query_pairs.get("c"));
              } else if (parts.length == 3) {
                  a = Double.parseDouble(parts[0]);
                  b = Double.parseDouble(parts[1]);
                  c = Double.parseDouble(parts[2]);
              } else {
                  throw new IllegalArgumentException("Invalid number of parameters");
              }
      
              // Calculate the ans
              double ans = b * b - 4 * a * c;
      
              // Check if roots are real
              if (ans >= 0) {
                  double root1 = (-b + Math.sqrt(ans)) / (2 * a);
                  double root2 = (-b - Math.sqrt(ans)) / (2 * a);
      
                  // Generate response
                  builder.append("HTTP/1.1 200 OK\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("Root 1: " + root1 + "\n");
                  builder.append("Root 2: " + root2);
              } else {
                  // Generate response for complex roots
                  builder.append("HTTP/1.1 200 OK\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("Ansers are complex numbers and I can not represent those");
              }
          } catch (Exception e) {
              // Handle invalid input or other exceptions
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Invalid input.Provide a, b, and c for a*x^2 + b*x + c = 0");
          }
      } else if (request.contains("github?")) {
            Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            query_pairs = splitQuery(request.replace("github?", ""));
            try {
                String json = fetchURL("https://api.github.com/" + query_pairs.get("query"));
                String parsedInfo = parseGitHub(json);
                System.out.println(json);
        
                builder.append("HTTP/1.1 200 OK\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append(parsedInfo);
            } catch (Exception e) 
            {
                builder.append("HTTP/1.1 500 Internal Server Error\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("Could not get github information at that URL...check your spelling and try again");
            }
        } else {
            // if the request is not recognized at all
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("I am not sure what you want me to do...");
        }      

        // Output
        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  /**
   * Method to read in a query and split it up correctly
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    // "q=hello+world%2Fme&bob=5"
    String[] pairs = query.split("&");
    // ["q=hello+world%2Fme", "bob=5"]
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    // {{"q", "hello world/me"}, {"bob","5"}}
    return query_pairs;
  }

  /**
   * Builds an HTML file list from the www directory
   * @return HTML string output of file list
   */
  public static String buildFileList() {
    ArrayList<String> filenames = new ArrayList<>();

    // Creating a File object for directory
    File directoryPath = new File("www/");
    filenames.addAll(Arrays.asList(directoryPath.list()));

    if (filenames.size() > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("<ul>\n");
      for (var filename : filenames) {
        builder.append("<li>" + filename + "</li>");
      }
      builder.append("</ul>\n");
      return builder.toString();
    } else {
      return "No files in directory";
    }
  }

  /**
   * Read bytes from a file and return them in the byte array. We read in blocks
   * of 512 bytes for efficiency.
   */
  public static byte[] readFileInBytes(File f) throws IOException {

    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

    byte buffer[] = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();

    byte[] result = data.toByteArray();
    data.close();

    return result;
  }

  /**
   *
   * a method to make a web request. Note that this method will block execution
   * for up to 20 seconds while the request is being satisfied. Better to use a
   * non-blocking request.
   * 
   * @param aUrl the String indicating the query url for the OMDb api search
   * @return the String result of the http request.
   *
   **/
  public String fetchURL(String aUrl) {
    StringBuilder sb = new StringBuilder();
    URLConnection conn = null;
    InputStreamReader in = null;
    try {
      URL url = new URL(aUrl);
      conn = url.openConnection();
      if (conn != null)
        conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
      if (conn != null && conn.getInputStream() != null) {
        in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
        BufferedReader br = new BufferedReader(in);
        if (br != null) {
          int ch;
          // read the next character until end of reader
          while ((ch = br.read()) != -1) {
            sb.append((char) ch);
          }
          br.close();
        }
      }
      in.close();
    } catch (Exception ex) {
      System.out.println("Exception in url request:" + ex.getMessage());
    }
    return sb.toString();
  }

  public static String parseGitHub(String json) 
  {
    StringBuilder parsedInfo = new StringBuilder();
      json = json.substring(1, json.length() - 1);
      String[] repos = json.split("\\},\\{"); //json delimiters found on stack
      for (String repo : repos) 
      {
        String fullName = getValue(repo, "full_name");
        int id = Integer.parseInt(getValue(repo, "id"));
        String login = getValue(repo, "login");

        parsedInfo.append("Full Name: ").append(fullName).append("\n");
        parsedInfo.append("ID: ").append(id).append("\n");
        parsedInfo.append("Login: ").append(login).append("\n\n");
      }
    return parsedInfo.toString();
  }

  private static String getValue(String json, String key) 
  {
      String[] parts = json.split(",");
      for (String part : parts) 
      {
          if (part.contains(key)) {
              if (key.equals("login") && part.contains("owner")) 
              {
                  String[] keyValue = part.split(":");
                  return keyValue[keyValue.length - 1].trim().replace("\"", "");
              } else {
                  String[] keyValue = part.split(":");
                  return keyValue[1].trim().replace("\"", "");
              }
          }
      }
      return "";
  }
}
