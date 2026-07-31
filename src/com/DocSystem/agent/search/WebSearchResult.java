package com.DocSystem.agent.search;

/**
 * 网络搜索结果条目（T8.4）。
 */
public class WebSearchResult {

    /** 结果标题 */
    public final String title;
    /** 结果链接（已解码为可直接访问的 URL） */
    public final String url;
    /** 结果摘要 */
    public final String snippet;

    public WebSearchResult(String title, String url, String snippet) {
        this.title = title;
        this.url = url;
        this.snippet = snippet;
    }
}
