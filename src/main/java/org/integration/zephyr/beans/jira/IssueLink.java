package org.integration.zephyr.beans.jira;

public class IssueLink {

    private Metafield type;
    private Metafield inwardIssue;
    private Metafield outwardIssue;

    public IssueLink(String source, String target, String type) {
        this.type = new Metafield();
        this.type.setName(type);
        this.inwardIssue = new Metafield();
        this.inwardIssue.setKey(source);
        this.outwardIssue = new Metafield();
        this.outwardIssue.setKey(target);
    }

    public Metafield getType() {
        return type;
    }

    public void setType(Metafield type) {
        this.type = type;
    }

    public Metafield getInwardIssue() {
        return inwardIssue;
    }

    public void setInwardIssue(Metafield inwardIssue) {
        this.inwardIssue = inwardIssue;
    }

    public Metafield getOutwardIssue() {
        return outwardIssue;
    }

    public void setOutwardIssue(Metafield outwardIssue) {
        this.outwardIssue = outwardIssue;
    }
}
