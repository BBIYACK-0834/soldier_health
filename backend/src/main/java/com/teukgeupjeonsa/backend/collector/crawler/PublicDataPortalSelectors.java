package com.teukgeupjeonsa.backend.collector.crawler;

public final class PublicDataPortalSelectors {
    private PublicDataPortalSelectors() {}

    public static final String DATASET_ITEM = "li.result-list, div.result-list, li.data-list";
    public static final String TITLE_LINK = "a[href*='selectDataSetDetail']";
    public static final String PROVIDER = ".result-info, .data-info, .item-info";
    public static final String DESCRIPTION = ".result-description, .desc, p";
    public static final String DETAIL_DOWNLOAD_LINKS = "a[href], button[data-url]";
}
