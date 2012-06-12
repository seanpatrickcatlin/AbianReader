/*
This file is part of AbianReader.

AbianReader is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

AbianReader is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with AbianReader.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.abiansoftware.lib.reader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import com.abiansoftware.lib.reader.AbianReaderData.AbianReaderItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.abiansoftware.lib.reader.R;
import com.loopj.android.http.*;

public class AbianReaderActivity extends Activity implements OnClickListener {
	private static final String XML_FILE_NAME = "AbianReaderPreferences.xml";

	private static final String TAG = "AbianReaderActivity";
	private static final String RSS_ITEM = "item";
	private static final String RSS_TITLE = "title";
	private static final String RSS_LINK = "link";
	private static final String RSS_DESCRIPTION = "description";
	private static final String RSS_CONTENT = "content:encoded";
	private static final String RSS_PUB_DATE = "pubDate";
	private static final String RSS_CREATOR = "dc:creator";
	private static final String RSS_COMMENTS = "comments";
	private static final String RSS_COMMENT_COUNT = "slash:comments";

	private static final String XML_PAGE = "abianReaderXmlPage";
	private static final String XML_TIME = "abianReaderXmlTime";
	private static final String XML_ROOT = "abianReaderXmlRoot";
	private static final String XML_CREATOR = "abianReaderXmlCreator";
	private static final String XML_CONTENT = "abianReaderXmlContent";
	private static final String XML_COMMENT_COUNT = "abianReaderXmlCommentCount";
	private static final String XML_AUTO_UPDATE_TIME = "abianReaderXmlAutoUpdateTime";
	private static final String XML_THUMBNAIL_LINK = "abianReaderXmlThumbnailLink";
	private static final String XML_FEATURED_IMAGE_LINK = "abianReaderXmlFeaturedLink";
	private static final String XML_IS_FEATURED = "abianReaderXmlIsFeatured";

	private static final String AUTO_UPDATE_ONE_MINUTE = "1 Minute";
	private static final String AUTO_UPDATE_FIVE_MINUTES = "5 Minutes";
	private static final String AUTO_UPDATE_FIFTEEN_MINUTES = "15 Minutes";
	private static final String AUTO_UPDATE_THIRTY_MINUTES = "30 Minutes";
	private static final String AUTO_UPDATE_ONE_HOUR = "1 Hour";
	private static final String AUTO_UPDATE_FOUR_HOURS = "4 Hours";
	private static final String AUTO_UPDATE_TWELVE_HOURS = "12 Hours";
	private static final String AUTO_UPDATE_ONE_DAY = "1 Day";

	private static final int MSG_UPDATE_LIST = 22609;

	private static boolean s_bTryJson = false;
	private static boolean s_bUseJsonContent = false;

	private String m_featuredTag;

	private static final CharSequence[] AUTO_UPDATE_OPTIONS = {
			AUTO_UPDATE_ONE_MINUTE, AUTO_UPDATE_FIVE_MINUTES,
			AUTO_UPDATE_FIFTEEN_MINUTES, AUTO_UPDATE_THIRTY_MINUTES,
			AUTO_UPDATE_ONE_HOUR, AUTO_UPDATE_FOUR_HOURS,
			AUTO_UPDATE_TWELVE_HOURS, AUTO_UPDATE_ONE_DAY };

	public static int s_width = 100;
	public static int s_height = 100;

	private static AbianReaderActivity s_singleton = null;

	private AbianReaderData m_abianReaderData;
	private RefreshFeedTask m_refreshFeedTask;

	private AbianReaderListView m_rssFeedListView;
	private AbianReaderItemView m_rssItemView;

	private ImageView m_refreshImageView;
	private ImageView m_headerImageView;
	private ImageView m_autoUpdateImageView;

	private boolean m_bIsRefreshingFeed;

	private int m_preferredListItemHeight;

	private static AsyncHttpClient s_asyncHttpClient = new AsyncHttpClient();

	private Handler m_appHandler;

	public static void doHttpGet(String url, RequestParams params,
			AsyncHttpResponseHandler responseHandler) {
		s_asyncHttpClient.get(url, params, responseHandler);
	}

	public static void doHttpBinaryGet(String url,
			BinaryHttpResponseHandler responseHandler) {
		s_asyncHttpClient.get(url, responseHandler);
	}

	public static void doHttpPost(String url, RequestParams params,
			AsyncHttpResponseHandler responseHandler) {
		s_asyncHttpClient.post(url, params, responseHandler);
	}

	public String getFeaturedTag() {
		return m_featuredTag;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		m_bIsRefreshingFeed = true;

		m_refreshFeedTask = null;
		m_abianReaderData = new AbianReaderData();

		m_preferredListItemHeight = 40;

		s_singleton = this;

		m_appHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MSG_UPDATE_LIST) {
					updateRssFeedList(false);
				}
			}
		};

		m_featuredTag = getString(R.string.featured_tag);

		String tryJsonStr = getString(R.string.try_json);

		if ((tryJsonStr != null) && (tryJsonStr.length() > 0)) {
			s_bTryJson = true;
		}

		// turn off the window's title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// set the view
		setContentView(R.layout.abian_reader_activity);

		m_rssItemView = (AbianReaderItemView) findViewById(R.id.abian_reader_item_view);
		m_rssFeedListView = (AbianReaderListView) findViewById(R.id.abian_reader_list_view);

		LinearLayout appHeaderLinearLayout = (LinearLayout) findViewById(R.id.app_header_linear_layout);

		ViewGroup.LayoutParams appHeaderLayoutParams = appHeaderLinearLayout
				.getLayoutParams();
		m_preferredListItemHeight = appHeaderLayoutParams.height;
		Log.e(TAG, "Preferred Item height: " + m_preferredListItemHeight);

		m_refreshImageView = (ImageView) findViewById(R.id.refresh_button);
		m_headerImageView = (ImageView) findViewById(R.id.header_button);
		m_autoUpdateImageView = (ImageView) findViewById(R.id.auto_update_button);

		m_refreshImageView.setOnClickListener(this);
		m_headerImageView.setOnClickListener(this);
		m_autoUpdateImageView.setOnClickListener(this);

		m_rssItemView.initializeViewAfterPopulation(this);
		m_rssFeedListView.initializeViewAfterPopulation(this);

		// load the RSSData from disk
		new LoadDataFromFileTask().execute();

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		s_width = metrics.widthPixels;
		s_height = metrics.heightPixels;
	}

	static public void updateListPlease() {
		s_singleton.m_appHandler.sendEmptyMessage(MSG_UPDATE_LIST);
	}

	static public boolean getTryJson() {
		return s_bTryJson;
	}

	static public AbianReaderActivity getSingleton() {
		return s_singleton;
	}

	static public AbianReaderData getData() {
		if (s_singleton != null) {
			return s_singleton.getReaderData();
		} else {
			return null;
		}
	}

	public int getPreferredListItemHeight() {
		return m_preferredListItemHeight;
	}

	public AbianReaderData getReaderData() {
		return m_abianReaderData;
	}

	public boolean isRefreshingFeed() {
		return m_bIsRefreshingFeed;
	}

	private void updateRssFeedList(boolean bClear) {
		m_rssFeedListView.updateList(bClear);
	}

	public void getNextFeed() {
		m_abianReaderData.setPageNumber(m_abianReaderData.getPageNumber() + 1);
		refreshFeed();
	}

	public void getPrevFeed() {
		m_abianReaderData.setPageNumber(m_abianReaderData.getPageNumber() - 1);
		refreshFeed();
	}

	public void refreshFeed() {
		if ((m_refreshFeedTask == null)
				|| (m_refreshFeedTask.getStatus() == AsyncTask.Status.FINISHED)) {
			try {
				m_bIsRefreshingFeed = true;

				m_abianReaderData.clear();
				updateRssFeedList(true);

				m_refreshFeedTask = new RefreshFeedTask();

				String feedUrl = s_singleton.getString(R.string.feed_url_str);

				if (s_bTryJson && s_bUseJsonContent) {
					feedUrl = s_singleton.getString(R.string.site_url_str);
					feedUrl += "/?json=1&count="
							+ m_abianReaderData.getNumberOfItems() + "&page="
							+ m_abianReaderData.getPageNumber();
				} else {
					if (feedUrl.contains("?")) {
						feedUrl += "&";
					} else {
						feedUrl += "/?";
					}

					feedUrl += "paged=" + m_abianReaderData.getPageNumber();
				}

				m_refreshFeedTask.execute(feedUrl);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		}
	}

	public void showRssItemContent(int position) {
		m_rssItemView.setTargetRssItem(position);
		m_rssFeedListView.setVisibility(View.GONE);
		m_rssItemView.setVisibility(View.VISIBLE);

		m_refreshImageView.setImageResource(R.drawable.share);
		m_autoUpdateImageView.setImageResource(R.drawable.comments);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
			if (m_rssItemView.getVisibility() == View.VISIBLE) {
				m_rssItemView.clearWebView();
				m_rssItemView.setVisibility(View.GONE);
				m_rssFeedListView.setVisibility(View.VISIBLE);

				m_refreshImageView.setImageResource(R.drawable.refresh);
				m_autoUpdateImageView.setImageResource(R.drawable.settings);

				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	private boolean isNetworkAvailable() {
		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();

		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI")) {
				if (ni.isConnected()) {
					haveConnectedWifi = true;
				}
			}

			if (ni.getTypeName().equalsIgnoreCase("MOBILE")) {
				if (ni.isConnected()) {
					haveConnectedMobile = true;
				}
			}
		}

		return haveConnectedWifi || haveConnectedMobile;
	}

	private class RefreshFeedTask extends AsyncTask<String, Void, Void> {
		private ProgressDialog m_progressDialog = null;

		@Override
		protected Void doInBackground(String... params) {
			String urlToConnectTo = params[0];

			try {
				URL urlObject = new URL(urlToConnectTo);
				HttpURLConnection httpConnection = (HttpURLConnection) urlObject
						.openConnection();
				httpConnection.setConnectTimeout(30 * 1000);
				httpConnection.setReadTimeout(30 * 1000);

				if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
					Log.d(TAG,
							"getInputStream() HTTP returning null.  Received HTTP response code: "
									+ httpConnection.getResponseCode()
									+ ", Url: " + urlToConnectTo);
					return null;
				}

				InputStream is = httpConnection.getInputStream();

				if (is != null) {
					if (s_bTryJson && s_bUseJsonContent) {
						parseJsonFeed(is);
					} else {
						SAXParserFactory theSaxParserFactory = SAXParserFactory
								.newInstance();
						SAXParser theSaxParser = theSaxParserFactory
								.newSAXParser();
						XMLReader theXmlReader = theSaxParser.getXMLReader();

						theXmlReader.setContentHandler(new RSSFeedHandler());

						theXmlReader.parse(new InputSource(is));
					}
				}
			} catch (Exception e) {
				Log.w(getClass().getName(), "Exception thrown: " + e.toString()
						+ ", Url: " + urlToConnectTo);
			}

			return null;
		}

		@Override
		protected void onPreExecute() {
			AbianReaderData rssData = AbianReaderActivity.getData();

			if (rssData != null) {
				rssData.clear();
			}

			if (isNetworkAvailable()) {
				String loadingTitle = s_singleton
						.getString(R.string.loading_title_str);
				String loadingMsg = s_singleton
						.getString(R.string.loading_message_str);

				m_progressDialog = ProgressDialog.show(s_singleton,
						loadingTitle, loadingMsg);
				m_progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				m_progressDialog.setMax(2);
				m_progressDialog.setProgress(0);
			} else {
				cancel(true);

				String toastText = s_singleton
						.getString(R.string.no_network_str);

				Toast.makeText(s_singleton, toastText, Toast.LENGTH_SHORT);
			}
		}

		@Override
		protected void onPostExecute(Void param) {
			m_progressDialog.setProgress(2);
			m_progressDialog.dismiss();
			m_progressDialog = null;

			m_bIsRefreshingFeed = false;

			boolean bWarnEmpty = false;

			AbianReaderData rssData = AbianReaderActivity.getData();

			if (rssData != null) {
				rssData.setLastUpdateTimeToNow();

				updateRssFeedList(false);

				if (rssData.getNumberOfItems() == 0) {
					bWarnEmpty = true;
				}
			} else {
				bWarnEmpty = true;
			}

			if (bWarnEmpty) {
				String toastText = s_singleton.getString(R.string.no_items_str);

				Toast.makeText(s_singleton, toastText, Toast.LENGTH_SHORT);
			} else {
				if (s_bTryJson && !s_bUseJsonContent) {
					new GetJsonDataTask().execute((Void) null);
				}
			}
		}
	}

	private void parseJsonFeed(InputStream is) {
		try {
			// just get all of the data and store it in a string
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr, (1024 * 2));
			StringBuilder sb = new StringBuilder();

			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}

			is.close();

			JSONObject rootJsonObject = new JSONObject(sb.toString());

			JSONArray postArray = rootJsonObject.getJSONArray("posts");

			for (int i = 0; i < postArray.length(); i++) {
				JSONObject thisPostObject = postArray.getJSONObject(i);

				AbianReaderItem newItem = new AbianReaderItem();

				newItem.setTitle(thisPostObject.getString("title_plain"));
				newItem.setDescription(thisPostObject.getString("excerpt"));
				newItem.setLink(thisPostObject.getString("url"));
				newItem.setContent(thisPostObject.getString("content"));
				newItem.setJsonDate(thisPostObject.getString("date"));

				JSONObject thisPostAuthor = thisPostObject
						.getJSONObject("author");
				newItem.setCreator(thisPostAuthor.getString("name"));
				newItem.setCommentsLink(newItem.getLink());

				JSONArray thisCommentArray = thisPostObject
						.getJSONArray("comments");
				newItem.setCommentCount(thisCommentArray.length());

				newItem.setThumbnailLink(thisPostObject.getString("thumbnail"));

				m_abianReaderData.addItem(newItem);
			}

		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	private class GetJsonDataTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			String feedUrl = s_singleton.getString(R.string.site_url_str);
			feedUrl += "/?json=1&page=";
			feedUrl += m_abianReaderData.getPageNumber();
			feedUrl += "&count=";
			feedUrl += m_abianReaderData.getNumberOfItems();
			feedUrl += "&include=thumbnail,url,attachments,tags";

			try {
				URL urlObject = new URL(feedUrl);
				HttpURLConnection httpConnection = (HttpURLConnection) urlObject
						.openConnection();
				httpConnection.setConnectTimeout(30 * 1000);
				httpConnection.setReadTimeout(30 * 1000);

				if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
					Log.d(TAG,
							"getInputStream() HTTP returning null.  Received HTTP response code: "
									+ httpConnection.getResponseCode()
									+ ", Url: " + urlObject.toString());
					return null;
				}

				InputStream is = httpConnection.getInputStream();

				if (is != null) {
					// just get all of the data and store it in a string
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr, (1024 * 2));
					StringBuilder sb = new StringBuilder();

					String line = null;
					try {
						while ((line = br.readLine()) != null) {
							sb.append(line + "\n");
						}

						is.close();
					} catch (Exception e) {
						Log.w(TAG,
								"Exception thrown in json downlaod: "
										+ e.toString() + ", Url: "
										+ urlObject.toString());
					}

					JSONObject rootJsonObject = new JSONObject(sb.toString());

					JSONArray postArray = rootJsonObject.getJSONArray("posts");

					for (int postPos = 0; postPos < postArray.length(); postPos++) {
						JSONObject thisPostObject = postArray
								.getJSONObject(postPos);

						boolean bThisPostIsFeatured = false;
						String thisPostLink = thisPostObject.getString("url");
						String thisPostThumbnailLink = thisPostObject
								.getString("thumbnail");
						String thisFeatureImageLink = "";

						if (thisPostObject.has("attachments")) {
							JSONArray attachmentArray = thisPostObject
									.getJSONArray("attachments");

							for (int attachmentPos = 0; attachmentPos < attachmentArray
									.length(); attachmentPos++) {
								JSONObject thisAttachment = attachmentArray
										.getJSONObject(attachmentPos);

								if (thisAttachment.has("images")) {
									JSONObject imagesObject = thisAttachment
											.getJSONObject("images");

									JSONObject fullImageObject = imagesObject
											.getJSONObject("full");
									String thisUrl = fullImageObject
											.getString("url");

									if (thisFeatureImageLink.length() == 0) {
										thisFeatureImageLink = thisUrl;
									} else {
										int matchingCurrentFeature = getNumberOfSameCharacters(
												thisPostThumbnailLink,
												thisFeatureImageLink);
										int matchingThisFeature = getNumberOfSameCharacters(
												thisPostThumbnailLink, thisUrl);

										if (matchingThisFeature > matchingCurrentFeature) {
											thisFeatureImageLink = thisUrl;
										}
									}
								}
							}
						}

						if ((m_abianReaderData.getPageNumber() == 1)
								&& (thisPostObject.has("tags"))) {
							JSONArray tagArray = thisPostObject
									.getJSONArray("tags");

							for (int tagPos = 0; tagPos < tagArray.length(); tagPos++) {
								JSONObject thisTag = tagArray
										.getJSONObject(tagPos);

								String tagName = thisTag.getString("title");

								if (tagName.equalsIgnoreCase(m_featuredTag)) {
									bThisPostIsFeatured = true;
									Log.e(TAG, "Featured: ");
									break;
								}
							}
						}

						for (int rssItemPos = 0; rssItemPos < m_abianReaderData
								.getNumberOfItems(); rssItemPos++) {
							if (m_abianReaderData.getItemNumber(rssItemPos)
									.getLink().equalsIgnoreCase(thisPostLink)) {
								m_abianReaderData
										.getItemNumber(rssItemPos)
										.setThumbnailLink(thisPostThumbnailLink);
								m_abianReaderData.getItemNumber(rssItemPos)
										.setFeaturedImageLink(
												thisFeatureImageLink,
												bThisPostIsFeatured);
							}
						}
					}
				}
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}

			return null;
		}

		protected void onProgressUpdate(Void... params) {
			updateRssFeedList(false);
		}
	}

	private class RSSFeedHandler extends DefaultHandler {
		private AbianReaderData m_abianReaderData = null;
		private StringBuffer m_currentCharactersStringBuffer;

		private String m_title;
		private String m_link;
		private String m_description;
		private String m_content;
		private String m_pubDate;
		private String m_creator;
		private String m_comments;
		private String m_thumbnailLink;
		private String m_featuredImageLink;
		private boolean m_bIsFeatured;
		private int m_commentCount;

		@Override
		public void startDocument() throws SAXException {
			m_abianReaderData = AbianReaderActivity.getData();
			m_abianReaderData.clear();
		}

		@Override
		public void endDocument() throws SAXException {
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (qName.compareTo(RSS_ITEM) == 0) {
				m_title = "";
				m_description = "";
				m_link = "";
				m_content = "";
				m_pubDate = "";
				m_creator = "";
				m_comments = "";
				m_thumbnailLink = "";
				m_featuredImageLink = "";
				m_bIsFeatured = false;
				m_commentCount = 0;

				m_currentCharactersStringBuffer = new StringBuffer();
			} else if ((qName.compareTo(RSS_TITLE) == 0)
					|| (qName.compareTo(RSS_LINK) == 0)
					|| (qName.compareTo(RSS_DESCRIPTION) == 0)
					|| (qName.compareTo(RSS_CONTENT) == 0)
					|| (qName.compareTo(RSS_PUB_DATE) == 0)
					|| (qName.compareTo(RSS_CREATOR) == 0)
					|| (qName.compareTo(RSS_COMMENTS) == 0)
					|| (qName.compareTo(RSS_COMMENT_COUNT) == 0)
					|| (qName.compareTo(XML_PAGE) == 0)
					|| (qName.compareTo(XML_TIME) == 0)
					|| (qName.compareTo(XML_CONTENT) == 0)
					|| (qName.compareTo(XML_THUMBNAIL_LINK) == 0)
					|| (qName.compareTo(XML_FEATURED_IMAGE_LINK) == 0)
					|| (qName.compareTo(XML_IS_FEATURED) == 0)
					|| (qName.compareTo(XML_CREATOR) == 0)
					|| (qName.compareTo(XML_COMMENT_COUNT) == 0)
					|| (qName.compareTo(XML_AUTO_UPDATE_TIME) == 0)) {
				m_currentCharactersStringBuffer = new StringBuffer();
			} else {
				m_currentCharactersStringBuffer = null;
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (m_currentCharactersStringBuffer != null) {
				// just append to our string buffer
				m_currentCharactersStringBuffer.append(ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			String thisText = "";

			if (m_currentCharactersStringBuffer != null) {
				thisText = m_currentCharactersStringBuffer.toString();
			}

			if (qName.compareTo(RSS_ITEM) == 0) {
				AbianReaderItem newItem = new AbianReaderItem();

				newItem.setTitle(m_title);
				newItem.setDescription(m_description);
				newItem.setLink(m_link);
				newItem.setContent(m_content);
				newItem.setPubDate(m_pubDate);
				newItem.setCreator(m_creator);
				newItem.setCommentsLink(m_comments);
				newItem.setCommentCount(m_commentCount);
				newItem.setThumbnailLink(m_thumbnailLink);
				newItem.setFeaturedImageLink(m_featuredImageLink, m_bIsFeatured);

				m_abianReaderData.addItem(newItem);

				m_title = "";
				m_description = "";
				m_link = "";
				m_content = "";
				m_pubDate = "";
				m_creator = "";
				m_comments = "";
				m_thumbnailLink = "";
				m_featuredImageLink = "";
				m_bIsFeatured = false;
				m_commentCount = 0;
			} else if (qName.compareTo(RSS_TITLE) == 0) {
				m_title = thisText;
			} else if (qName.compareTo(RSS_LINK) == 0) {
				m_link = thisText;
			} else if (qName.compareTo(RSS_DESCRIPTION) == 0) {
				m_description = thisText;
			} else if ((qName.compareTo(RSS_CONTENT) == 0)
					|| (qName.compareTo(XML_CONTENT) == 0)) {
				m_content = thisText;
			} else if (qName.compareTo(RSS_PUB_DATE) == 0) {
				m_pubDate = thisText;
			} else if ((qName.compareTo(RSS_CREATOR) == 0)
					|| (qName.compareTo(XML_CREATOR) == 0)) {
				m_creator = thisText;
			} else if (qName.compareTo(RSS_COMMENTS) == 0) {
				m_comments = thisText;
			} else if (qName.compareTo(XML_THUMBNAIL_LINK) == 0) {
				m_thumbnailLink = thisText;
			} else if (qName.compareTo(XML_FEATURED_IMAGE_LINK) == 0) {
				m_featuredImageLink = thisText;
			} else if (qName.compareTo(XML_IS_FEATURED) == 0) {
				m_bIsFeatured = Boolean.valueOf(thisText).booleanValue();
			} else if ((qName.compareTo(RSS_COMMENT_COUNT) == 0)
					|| (qName.compareTo(XML_COMMENT_COUNT) == 0)) {
				try {
					m_commentCount = Integer.parseInt(thisText);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			} else if (qName.compareTo(XML_PAGE) == 0) {
				try {
					m_abianReaderData.setPageNumber(Integer.parseInt(thisText));
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			} else if (qName.compareTo(XML_TIME) == 0) {
				try {
					m_abianReaderData.setLastUpdateTime(Long
							.parseLong(thisText));
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			} else if (qName.compareTo(XML_AUTO_UPDATE_TIME) == 0) {
				try {
					m_abianReaderData.setAutoUpdateTimeInMinutes(Integer
							.parseInt(thisText));
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			}
		}

		@Override
		public void error(SAXParseException e) throws SAXException {
			Log.e(TAG, "SAX error: " + e.toString());
		}

		@Override
		public void warning(SAXParseException e) throws SAXException {
			Log.w(TAG, "SAX warning: " + e.toString());
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.refresh_button) {
			if (m_rssItemView.getVisibility() == View.VISIBLE) {
				int itemPosition = m_rssItemView.getTargetRssItem();

				AbianReaderItem targetItem = m_abianReaderData
						.getItemNumber(itemPosition);

				if (targetItem != null) {
					String shareMessage = getString(R.string.share_message);
					String shareTitle = getString(R.string.share_title);

					Intent sharingIntent = new Intent(
							android.content.Intent.ACTION_SEND);
					sharingIntent.setType("text/plain");
					sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareMessage);
					sharingIntent.putExtra(Intent.EXTRA_TEXT,
							targetItem.getLink());
					startActivity(Intent.createChooser(sharingIntent,
							shareTitle));
				}
			} else {
				refreshFeed();
			}
		} else if (v.getId() == R.id.header_button) {
			AbianReaderActivity
					.openUrlInBrowser(getString(R.string.site_url_str));
		} else if (v.getId() == R.id.auto_update_button) {
			if (m_rssItemView.getVisibility() == View.VISIBLE) {
				int itemPosition = m_rssItemView.getTargetRssItem();

				AbianReaderItem targetItem = m_abianReaderData
						.getItemNumber(itemPosition);

				if (targetItem != null) {
					AbianReaderActivity.openUrlInBrowser(targetItem
							.getCommentsLink());
				}
			} else {
				AlertDialog.Builder thisAlertDialogBuilder = new AlertDialog.Builder(
						this);

				String currentAutoupdateString = getStringForMinuteValue(m_abianReaderData
						.getAutoUpdateTimeInMinutes());
				int defaultPosition = getIndexOfLabel(currentAutoupdateString);

				thisAlertDialogBuilder.setTitle(R.string.update_frequency_str);
				thisAlertDialogBuilder.setSingleChoiceItems(
						AbianReaderActivity.AUTO_UPDATE_OPTIONS,
						defaultPosition, null);
				thisAlertDialogBuilder.setNegativeButton(R.string.cancel_str,
						null);
				thisAlertDialogBuilder.setPositiveButton(R.string.ok_str,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.dismiss();
								int selectedPosition = ((AlertDialog) dialog)
										.getListView().getCheckedItemPosition();

								String selectedString = getLabelOfIndex(selectedPosition);

								int selectedMinuteValue = getMinuteValueForString(selectedString);

								m_abianReaderData
										.setAutoUpdateTimeInMinutes(selectedMinuteValue);
							}
						});

				thisAlertDialogBuilder.show();
			}
		}
	}

	public static String getStringForMinuteValue(int minuteValue) {
		if (minuteValue == 1) {
			return AbianReaderActivity.AUTO_UPDATE_ONE_MINUTE;
		} else if (minuteValue == 5) {
			return AbianReaderActivity.AUTO_UPDATE_FIVE_MINUTES;
		} else if (minuteValue == 15) {
			return AbianReaderActivity.AUTO_UPDATE_FIFTEEN_MINUTES;
		} else if (minuteValue == 30) {
			return AbianReaderActivity.AUTO_UPDATE_THIRTY_MINUTES;
		} else if (minuteValue == 60) {
			return AbianReaderActivity.AUTO_UPDATE_ONE_HOUR;
		} else if (minuteValue == (4 * 60)) {
			return AbianReaderActivity.AUTO_UPDATE_FOUR_HOURS;
		} else if (minuteValue == (12 * 60)) {
			return AbianReaderActivity.AUTO_UPDATE_TWELVE_HOURS;
		} else if (minuteValue == (24 * 60)) {
			return AbianReaderActivity.AUTO_UPDATE_ONE_DAY;
		}

		return AbianReaderActivity.AUTO_UPDATE_ONE_DAY;
	}

	public int getMinuteValueForString(String targetString) {
		if (targetString.compareTo(AbianReaderActivity.AUTO_UPDATE_ONE_MINUTE) == 0) {
			return 1;
		} else if (targetString
				.compareTo(AbianReaderActivity.AUTO_UPDATE_FIVE_MINUTES) == 0) {
			return 5;
		} else if (targetString
				.compareTo(AbianReaderActivity.AUTO_UPDATE_FIFTEEN_MINUTES) == 0) {
			return 15;
		} else if (targetString
				.compareTo(AbianReaderActivity.AUTO_UPDATE_THIRTY_MINUTES) == 0) {
			return 30;
		} else if (targetString
				.compareTo(AbianReaderActivity.AUTO_UPDATE_ONE_HOUR) == 0) {
			return 60;
		} else if (targetString
				.compareTo(AbianReaderActivity.AUTO_UPDATE_FOUR_HOURS) == 0) {
			return (4 * 60);
		} else if (targetString
				.compareTo(AbianReaderActivity.AUTO_UPDATE_TWELVE_HOURS) == 0) {
			return (12 * 60);
		} else if (targetString
				.compareTo(AbianReaderActivity.AUTO_UPDATE_ONE_DAY) == 0) {
			return (24 * 60);
		}

		return (24 * 60);
	}

	public static int getIndexOfLabel(String labelString) {
		for (int i = 0; i < AbianReaderActivity.AUTO_UPDATE_OPTIONS.length; i++) {
			String thisLabel = AbianReaderActivity.AUTO_UPDATE_OPTIONS[i]
					.toString();

			if (thisLabel.compareTo(labelString) == 0) {
				return i;
			}
		}

		return 0;
	}

	public static String getLabelOfIndex(int theIndex) {
		if ((theIndex >= 0)
				&& (theIndex < AbianReaderActivity.AUTO_UPDATE_OPTIONS.length)) {
			return AbianReaderActivity.AUTO_UPDATE_OPTIONS[theIndex].toString();
		} else {
			return "";
		}
	}

	@Override
	protected void onStop() {
		saveRssDataToXml();

		super.onStop();
	}

	private class LoadDataFromFileTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				FileInputStream is = openFileInput(XML_FILE_NAME);

				if (is != null) {
					SAXParserFactory theSaxParserFactory = SAXParserFactory
							.newInstance();
					SAXParser theSaxParser = theSaxParserFactory.newSAXParser();
					XMLReader theXmlReader = theSaxParser.getXMLReader();

					theXmlReader.setContentHandler(new RSSFeedHandler());

					theXmlReader.parse(new InputSource(is));
				}
			} catch (Exception e) {
				Log.w(getClass().getName(), "Exception thrown: " + e.toString()
						+ ", File");
			}

			return null;
		}

		@Override
		protected void onPreExecute() {
			m_bIsRefreshingFeed = true;
		}

		@Override
		protected void onPostExecute(Void param) {
			// check the times and update if needed
			Time thisTime = new Time();
			thisTime.setToNow();

			long thisTimeInMillisNow = thisTime.toMillis(true);
			long lastUpdateMillis = m_abianReaderData.getLastUpdateTime()
					.toMillis(true);

			long millisInOneMinute = (60 * 1000);

			int autoUpdateTimeInMinutes = m_abianReaderData
					.getAutoUpdateTimeInMinutes();

			m_bIsRefreshingFeed = false;

			// if it has been more than a day update the articles
			if ((thisTimeInMillisNow - lastUpdateMillis) > (millisInOneMinute * autoUpdateTimeInMinutes)) {
				refreshFeed();
			} else {
				updateRssFeedList(false);
			}
		}
	}

	private void saveRssDataToXml() {
		try {
			FileOutputStream fos = openFileOutput(XML_FILE_NAME,
					Context.MODE_PRIVATE);

			// we create a XmlSerializer in order to write xml data
			XmlSerializer serializer = Xml.newSerializer();

			// we set the FileOutputStream as output for the serializer, using
			// UTF-8 encoding
			serializer.setOutput(fos, "UTF-8");

			// Write <?xml declaration with encoding (if encoding not null) and
			// standalone flag (if standalone not null)
			serializer.startDocument(null, Boolean.valueOf(true));

			// start a tag called "root"
			serializer.startTag(null, XML_ROOT);

			serializer.startTag(null, XML_TIME);
			serializer.text(Long.toString(m_abianReaderData.getLastUpdateTime()
					.toMillis(true)));
			serializer.endTag(null, XML_TIME);

			serializer.startTag(null, XML_PAGE);
			serializer
					.text(Integer.toString(m_abianReaderData.getPageNumber()));
			serializer.endTag(null, XML_PAGE);

			serializer.startTag(null, XML_AUTO_UPDATE_TIME);
			serializer.text(Integer.toString(m_abianReaderData
					.getAutoUpdateTimeInMinutes()));
			serializer.endTag(null, XML_AUTO_UPDATE_TIME);

			for (int i = 0; i < m_abianReaderData.getNumberOfItems(); i++) {
				AbianReaderItem thisItem = m_abianReaderData.getItemNumber(i);

				serializer.startTag(null, RSS_ITEM);

				serializer.startTag(null, RSS_TITLE);
				serializer.text(thisItem.getTitle());
				serializer.endTag(null, RSS_TITLE);

				serializer.startTag(null, RSS_LINK);
				serializer.text(thisItem.getLink());
				serializer.endTag(null, RSS_LINK);

				serializer.startTag(null, RSS_DESCRIPTION);
				serializer.text(thisItem.getDescription());
				serializer.endTag(null, RSS_DESCRIPTION);

				serializer.startTag(null, XML_CONTENT);
				serializer.text(thisItem.getContent());
				serializer.endTag(null, XML_CONTENT);

				serializer.startTag(null, RSS_PUB_DATE);
				serializer.text(thisItem.getPubDateLong());
				serializer.endTag(null, RSS_PUB_DATE);

				serializer.startTag(null, XML_CREATOR);
				serializer.text(thisItem.getCreator());
				serializer.endTag(null, XML_CREATOR);

				serializer.startTag(null, RSS_COMMENTS);
				serializer.text(thisItem.getCommentsLink());
				serializer.endTag(null, RSS_COMMENTS);

				serializer.startTag(null, XML_COMMENT_COUNT);
				serializer.text(Integer.toString(thisItem.getCommentsCount()));
				serializer.endTag(null, XML_COMMENT_COUNT);

				serializer.startTag(null, XML_THUMBNAIL_LINK);
				serializer.text(thisItem.getThumbnailLink());
				serializer.endTag(null, XML_THUMBNAIL_LINK);

				serializer.startTag(null, XML_FEATURED_IMAGE_LINK);
				serializer.text(thisItem.getFeaturedImageLink());
				serializer.endTag(null, XML_FEATURED_IMAGE_LINK);

				serializer.startTag(null, XML_IS_FEATURED);
				serializer.text(Boolean.toString(thisItem.getIsFeatured()));
				serializer.endTag(null, XML_IS_FEATURED);

				serializer.endTag(null, RSS_ITEM);
			}

			serializer.endTag(null, XML_ROOT);
			serializer.endDocument();
			serializer.flush();
			fos.close();
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	public static void openUrlInBrowser(String targetUrl) {
		if ((targetUrl != null) && (targetUrl.length() > 0)
				&& (s_singleton != null)) {
			if ((!(targetUrl.startsWith("http://")) && !(targetUrl
					.startsWith("https://")))) {
				targetUrl = "http://" + targetUrl;
			}

			Intent thisIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(targetUrl));
			s_singleton.startActivity(thisIntent);
		}
	}

	private static int getNumberOfSameCharacters(String str1, String str2) {
		int retVal = 0;

		if ((str1 != null) && (str2 != null)) {
			for (int i = 0; ((i < str1.length()) && (i < str2.length())); i++) {
				if (str1.charAt(i) == str2.charAt(i)) {
					retVal++;
				} else {
					break;
				}
			}
		}

		return retVal;
	}
}
