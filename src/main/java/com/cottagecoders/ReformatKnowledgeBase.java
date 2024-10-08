package com.cottagecoders;

import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.ColumnPositionMappingStrategyBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zendesk.client.v2.Zendesk;
import org.zendesk.client.v2.model.hc.Article;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReformatKnowledgeBase {

  private static final String systemPrompt = """
          You will refactor HTML-based KB articles from an old format to a new format. 
          Produce standard html, do not use markdown. 
          Start with H2 headings for the sections. 
          The section headings to not need ID attributes. 
          Do not produce a title. 
          Do not preface the HTML output.
          Ensure that numbered and bulleted lists from the original content are preserved.
          Ensure the embedded examples and content in preformatted and code tags is preserved and included with the original text when creating the new article. 
          Ensure that the <pre> </pre> and <code> </code> tags are kept in the new article. 
          Ensure that embedded images are kept in the new article.
          Produce only the new html content, no preface, comment, or introduction in the generated response. 
          If nothing is found for a section, include the section header, and add a statement "Add additional content.". 
          Produce only the section headings previously listed, in the order specified. 
          Using the previous information, convert the HTML-based content to the new format. 
          """;
  private static final String preface = """
          Create a knowledge base article using these section headings - 
          """;
  private static final String howToSectionNames = """      
           Summary, Reported Issue, Overview, Relevant Versions Tools and Integrations, Steps to Resolve, Common Challenges, and Additional Resources. 
          """;
  private static final String troubleShootingSectionNames = """
          Summary, Reported Issue, Relevant Versions, Troubleshooting Steps, Cause, Steps to Resolve, Tips & Tricks, Best Practices, Recommendations, FAQ, Additional Resources. 
          Tips & Tricks, Best Practices, Recommendations, FAQ, and Additional Resources. 
          """;

  private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
  private static final String CLAUDE_API_KEY = System.getenv("CLAUDE_API_KEY");

  ReformatKnowledgeBase() {

  }

  public static void main(String[] argv) {
    long startTime = System.currentTimeMillis();
    ReformatKnowledgeBase rkb = new ReformatKnowledgeBase();
    try {
      rkb.run();
    } catch (IOException | InterruptedException ex) {
      System.err.println("Exception: " + ex.getMessage());
      ex.printStackTrace();
      System.exit(1);
    }

    long endTime = System.currentTimeMillis();
    System.out.println("Elapsed time: " + (endTime - startTime) + "ms");
    System.exit(0);
  }

  public static String sendClaudePrompt(String prompt) throws IOException, InterruptedException {
    // Rate Limiting
    HttpResponse<String> response;
    String text = "";

    do {
      HttpClient client = HttpClient.newHttpClient();
      JSONObject requestBody = new JSONObject();
      requestBody.put("model", "claude-3-sonnet-20240229");
      requestBody.put("max_tokens", 4096);
      requestBody.put("system", systemPrompt);
      requestBody.put("messages", new JSONObject[]{new JSONObject().put("role", "user").put("content", prompt)});

      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(CLAUDE_API_URL)).header("Content-Type",
                                                                                            "application/json").header(
              "x-api-key",
              System.getenv("CLAUDE_API_KEY")).header("anthropic-version",
                                                      "2023-06-01").POST(HttpRequest.BodyPublishers.ofString(requestBody.toString())).build();

      response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        System.out.println("Claude: API request failed with status code: " + response.statusCode());
        System.out.println("Response body: " + response.body());
        System.out.println("Sleep 60000 ");
        Thread.sleep(60000);
        continue;
      }

      JSONObject jsonResponse = new JSONObject(response.body());

      JSONArray contentArray = jsonResponse.getJSONArray("content");
      if (!contentArray.isEmpty()) {
        JSONObject contentObject = contentArray.getJSONObject(0);
        text = contentObject.getString("text");

      } else {
        System.out.println("The content array is empty.");

      }

    } while (response.statusCode() != 200);

    return text;
  }

  void run() throws IOException, InterruptedException {

    try (Zendesk zd = connectToZendesk()) {

      int count = 0;
      List<KBArticle> kba = new ArrayList<>();
      List<Article> toDo = new ArrayList<>();
      Set<String> allDrafts = new HashSet<>();

      for (Article a : zd.getArticles()) {
        toDo.add(a);
      }

      System.out.println(toDo.size() + " articles to process.");

      try (PrintWriter pw = new PrintWriter("output.html")) {
        // "toDo" is the list of all the articles to process.
        for (Article a : toDo) {
          String response = "";
          String assembledPrompt = "";

          // create the prompt
          if (zd.getSection(a.getSectionId()).getName().equals("How To")) {
            assembledPrompt = preface + howToSectionNames + " " + a.getBody();

          } else if (zd.getSection(a.getSectionId()).getName().equals("Troubleshooting")) {
            assembledPrompt = preface + troubleShootingSectionNames + " " + a.getBody();

          } else {
            // yes, we're using the "How To" section headings on all the mis-filed articles.
            assembledPrompt = preface + howToSectionNames + " " + a.getBody();

          }

          try {

            response = sendClaudePrompt(assembledPrompt);

            if (StringUtils.isEmpty(response)) {
              continue;
            }

            pw.println(response);

            // because the LocalAI LLM can't follow instructions:
            response = response.replace(
                    "Here is the converted HTML content to the specified Knowledge Base article format",
                    "");
            response = response.replace("Here is the knowledge base article with the requested sections and format:",
                                        "");
            response = response.replace("Here is the knowledge base article with the requested sections:", "");
            response = response.replace("Here is the rewritten HTML content using the specified section headings:", "");
            response = response.replace("Here is the rewritten content in the format you requested:", "");

          } catch (Exception e) {
            System.out.println("LLM Exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
          }

          Article replacement = new Article();
          replacement.setTitle(a.getTitle());
          if (a.getAuthorId() == 1267472396909L) { //permanently deleted user.
            a.setAuthorId(7595758108059L);   //bob
          }
          replacement.setAuthorId(a.getAuthorId());
          replacement.setSectionId(a.getSectionId());
          replacement.setContentTagIds(a.getContentTagIds());
          replacement.setTitle("Draft: " + a.getTitle());
          replacement.setBody(response);
          replacement.setDraft(true);
          replacement.setSectionId(a.getSectionId());
          replacement.setPermissionGroupId(a.getPermissionGroupId());
          replacement.setHtmlUrl("");
          replacement.setUrl("");
          zd.createArticle(replacement);

          String draftUrl = "";
          Iterable<Article> ans = zd.getArticles();
          for (Article poss : ans) {
            if (poss.getTitle().equals(replacement.getTitle())) {
              draftUrl = poss.getHtmlUrl();
            }
          }

          kba.add(new KBArticle(a.getTitle(), zd.getUser(a.getAuthorId()).getName(), a.getHtmlUrl(), draftUrl));
          ++count;
          System.out.println("count " + count);
        }

        writeCsvFile("./output.csv", kba);

      } catch (IOException ex) {
        System.out.println("Exception initializing PrintWriter: " + ex.getMessage());
        ex.printStackTrace();
        System.exit(1);

      }

    } catch (Exception ex) {
      System.out.println("Exception connecting to Zendesk: " + ex.getMessage());
      ex.printStackTrace();
      System.exit(5);
    }
  }

  void writeCsvFile(String fileName, List<KBArticle> recs) {

    try (PrintWriter pw = new PrintWriter(fileName)) {

      ColumnPositionMappingStrategy<KBArticle> strategy = new ColumnPositionMappingStrategyBuilder<KBArticle>().build();
      strategy.setType(KBArticle.class);
      StatefulBeanToCsv<KBArticle> sbc =
              new StatefulBeanToCsvBuilder<KBArticle>(pw).withQuotechar('\"').withMappingStrategy(
              strategy).withSeparator(CSVWriter.DEFAULT_SEPARATOR).build();

      sbc.write(recs);
    } catch (Exception ex) {
      System.out.println("Exception: " + ex.getMessage());
      ex.printStackTrace();
      System.exit(2);
    }
  }

  private Zendesk connectToZendesk() {
    return new Zendesk.Builder(System.getenv("ZENDESK_URL")).setUsername(System.getenv("ZENDESK_EMAIL")).setToken(System.getenv(
            "ZENDESK_TOKEN")).build();
  }
}
