package de.dala.simplenews.parser;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.Xml;

import com.android.volley.toolbox.StringRequest;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

import de.dala.simplenews.common.Category;
import de.dala.simplenews.common.Feed;
import de.dala.simplenews.common.News;
import de.dala.simplenews.R;
import de.dala.toasty.Toasty;

/**
 * Created by Daniel on 23.12.13.
 */
public class XmlParser  {
    /** TAG for logging **/
    private static String TAG="XmlParser";
    private String newsFeedResource = null;
    private int mNewsFeedResourceFileId = R.raw.categories;

    //--------------------------------------------------------------------------------
    //TAGs and ATTRIBUTEs in xml file
    //--------------------------------------------------------------------------------

    private static final String TAG_NEWS="news";
    private static final String TAG_CATEGORY="category";
    private static final String TAG_FEED="feed";

    private static final String ATTRIBUTE_NAME="name";
    private static final String ATTRIBUTE_COLOR="color";
    private static final String ATTRIBUTE_VISIBLE="visible";
    private static final String ATTRIBUTE_FEED_TITLE="title";

    //--------------------------------------------------------------------------------
    //Constructors
    //--------------------------------------------------------------------------------

    /**
     * Create a new instance for a context.
     *
     * @param context  current Context
     */
    private Context context;

    public XmlParser(Context context){
        this.context = context;
    }

    /**
     * Create a new instance for a context and for a custom news-file.
     *
     * You have to use file in res/raw folder.
     *
     * @param context  current Context
     * @param newsFeedResourceFileId  reference for a custom xml file
     */
    public XmlParser(Context context,int newsFeedResourceFileId){
        this.context = context;
        this.mNewsFeedResourceFileId =newsFeedResourceFileId;
    }

    /**
     * Create a new instance for a context and with a custom url .
     *
     * @param context  current Context
     * @param changeLogFileResourceUrl  url with xml files
     */
    public XmlParser(Context context,String changeLogFileResourceUrl){
        this.context = context;
        this.newsFeedResource =changeLogFileResourceUrl;
    }

    //--------------------------------------------------------------------------------


    /**
     * Read and parse res/raw/categories.xml or custom file
     *
     * @throws Exception if categories.xml or custom file is not found or if there are errors on parsing
     *
     * @return {@link News} obj with all data
     */
    public News readDefaultNewsFile() throws XmlPullParserException, IOException{

        News news = null;

        try {
            InputStream is;

            is = context.getResources().openRawResource(mNewsFeedResourceFileId);
            if (is!=null){

                // Create a new XML Pull Parser.
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(is, null);
                parser.nextTag();

                // Create changelog obj that will contain all data
                news=new News();
                // Parse file
                readNews(parser, news);

                // Close inputstream
                is.close();
            }else{
                Toasty.LOGD(TAG, "categories.xml not found");
            }
        } catch (XmlPullParserException xpe) {
            Toasty.LOGD(TAG,"XmlPullParseException while parsing changelog file",xpe);
            throw  xpe;
        } catch (IOException ioe){
            Toasty.LOGD(TAG,"Error i/o with categories.xml",ioe);
            throw ioe;
        }

        if (news!=null){
            Toasty.LOGD(TAG,"Process ended. News:" + news);
        }

        return news;
    }


    /**
     * Parse changelog node
     *
     * @param parser
     * @param news
     */
    protected void readNews(XmlPullParser parser, News news) throws  XmlPullParserException, IOException{

        if (parser==null || news==null) return;

        // Parse changelog node
        parser.require(XmlPullParser.START_TAG, null, TAG_NEWS);
        Toasty.LOGD(TAG,"Processing main tag=");

        //Parse nested nodes
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();
            Toasty.LOGD(TAG,"Processing tag="+tag);

            if (tag.equals(TAG_CATEGORY)) {
                readCategory(parser, news);
            }
        }
    }

    /**
     * Parse changeLogVersion node
     *
     * @param parser
     * @param news
     * @throws Exception
     */
    protected void readCategory(XmlPullParser parser, News news) throws  XmlPullParserException, IOException{

        if (parser==null) return;

        parser.require(XmlPullParser.START_TAG, null,TAG_CATEGORY);

        // Read attributes
        String categoryName = parser.getAttributeValue(null, ATTRIBUTE_NAME);
        String color= parser.getAttributeValue(null, ATTRIBUTE_COLOR);
        String visible = parser.getAttributeValue(null, ATTRIBUTE_VISIBLE);
        if (categoryName==null){
            //throw new ChangeLogException("VersionName required in changeLogVersion node");
        }

        Category category = new Category();
        category.setColor(Color.parseColor(color));
        category.setName(categoryName);
        if (visible != null){
            category.setVisible(!"false".equals(visible));
        }

        // Parse nested nodes
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag = parser.getName();
            Toasty.LOGD(TAG,"Processing tag="+tag);

            if (tag.equals(TAG_FEED)){
                Feed feed = readFeed(parser, news);
                if (feed != null){
                    category.getFeeds().add(feed);
                }
            }
        }
        news.getCategories().add(category);
    }

    /**
     *  Parse changeLogText node
     *
     * @param parser
     * @param news
     * @throws Exception
     */
    private Feed readFeed(XmlPullParser parser, News news) throws  XmlPullParserException, IOException{
        Feed feed = null;
        if (parser==null) return null;

        parser.require(XmlPullParser.START_TAG, null,TAG_FEED);

        String tag = parser.getName();
        if (tag.equals(TAG_FEED)){
            // Read attributes
            String feedTitle = parser.getAttributeValue(null,ATTRIBUTE_FEED_TITLE);
            String feedVisible = parser.getAttributeValue(null, ATTRIBUTE_VISIBLE);

            // Read text
            if (parser.next() == XmlPullParser.TEXT) {
                String feedText =parser.getText();
                if (feedText!=null){
                    feed = new Feed(feedText);
                    if (feedTitle!=null){
                        feed.setTitle(feedTitle);
                    }
                    if (feedVisible != null){
                        feed.setVisible(!"false".equals(feedVisible));
                    }
                }
                parser.nextTag();
            }

        }
        parser.require(XmlPullParser.END_TAG, null,TAG_FEED);
        return feed;
    }

    public String readShortenedLink(String shortenedLink) {
        int startIndex = shortenedLink.indexOf("<url>") + "<url>".length();
        int endIndex = shortenedLink.indexOf("</url>");
        if (startIndex > -1 && endIndex > -1){
            return shortenedLink.substring(startIndex,endIndex);
        }
        return null;
    }
}