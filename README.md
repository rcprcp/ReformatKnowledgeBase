Reformat Knowledge Base

This program extracts all the KB articles from the Zendesk Knowledge Base and creates a prompt and sens it to an LLM to be refoarmtted, e.g., different section headings, different content. 
## Environment
This program requires some environment variables to establish a connection to Zendesk and (optionally) to Slack: 
```shell
export ZENDESK_EMAIL="your.email@somewhere.com"
export ZENDESK_TOKEN="ffbglkfbYourZendeskTokenptrhb5jp42m"
export ZENDESK_URL="https://subdomain.zendesk.com"
export CLAUDE_API_KEY="ska-punk-rock556554563456-657567567=="
```
## Download the source
```shell
git clone https://github.com/rcprcp/ReformatKnowledgeBase.git
```

## Build 
Use the typical invocations for building a Maven-based application.
```shell
mvn clean package
```

Find the runnable jar in the `target` subdirectory: 
```shell
ls -ld ./target/ReformatKnowledgeBase-1.0-SNAPSHOT-jar-with-dependencies.jar
```