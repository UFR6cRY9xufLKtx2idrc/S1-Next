package me.ykrank.s1next.data.api;

import me.ykrank.s1next.data.User;

/**
 * Created by ykrank on 2017/1/4.
 */

public class ApiForum {

    static final String URL_FORUM = Api.BASE_API_PREFIX + "forumindex";

    static final String URL_THREAD_LIST = Api.BASE_API_PREFIX + "forumdisplay&version=4&filter=typeid&tpp=" + Api.THREADS_PER_PAGE;
    static final String URL_POST_LIST = Api.BASE_API_PREFIX + "viewthread&version=1&ppp=" + Api.POSTS_PER_PAGE;
    static final String URL_POST_LIST_NEW = Api.BASE_API_PREFIX + "viewthread&version=4&ppp=" + Api.POSTS_PER_PAGE;
    static final String URL_TRADE_POST_INFO = "forum.php?mod=viewthread&do=tradeinfo&inajax=1";

    /**
     * A URL used to get the correct authenticity token after login.
     * <p>
     * The {@link User#authenticityToken}
     * is not fresh if we have only logged in and haven't browsed
     * any new contents (which means requesting HTTP GET successfully).
     */
    static final String URL_AUTHENTICITY_TOKEN_HELPER = Api.BASE_API_PREFIX + "toplist";
    static final String URL_REPLY = Api.BASE_API_PREFIX + "sendreply&replysubmit=yes";

    /**
     * 发布新帖前获取必要前置信息
     */
    static final String URL_NEW_THREAD_HELPER = "forum.php?mod=post&action=newthread";
    static final String URL_NEW_THREAD = Api.BASE_API_PREFIX + "newthread&extra=&topicsubmit=yes";

    static final String URL_EDIT_POST_HELPER = "forum.php?mod=post&action=edit";
    static final String URL_EDIT_POST = "forum.php?mod=post&action=edit&editsubmit=yes&inajax=yes&wysiwyg=1&delete=0";
    static final String URL_QUOTE_POST_REDIRECT = "forum.php?mod=redirect&goto=findpost";
    /**
     * A URL to get the quoted user identification and processed quoted
     * content (with some HTML tags and its origin redirect hyperlink).
     */
    static final String URL_QUOTE_HELPER = "forum.php?mod=post&action=reply&inajax=yes";

    static final String URL_SEARCH_FORUM = "search.php?searchsubmit=yes&mod=forum";
    static final String URL_SEARCH_USER = "search.php?searchsubmit=yes&mod=user";

    static final String URL_VOTE = "forum.php?mod=misc&action=votepoll&pollsubmit=yes&quickforward=yes&inajax=1";

    static final String URL_RATE_LIST = "forum.php?mod=misc&action=viewratings&inajax=1";
}
