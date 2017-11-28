package org.innereye.router;

import org.innereye.gw.router.RouteResult;
import org.innereye.gw.router.Router;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

@ChannelHandler.Sharable
public class HttpRouterServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

    // For simplicity of this example, route targets are just simple strings.
    // But you can make them classes, and here once you get a target class,
    // you can create an instance of it and dispatch the request to the instance
    // etc.
    private final Router<String> router;

    HttpRouterServerHandler(Router<String> router){
        this.router = router;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest req) {
        if (HttpUtil.is100ContinueExpected(req)) {
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            return;
        }
        HttpResponse res = createResponse(req, router);
        flushResponse(ctx, req, res);
    }

    // Display debug info.
    private static HttpResponse createResponse(HttpRequest req, Router<String> router) {
        RouteResult<String> routeResult = router.route(req.method(), req.uri());
        String content = "router: \n" + router + "\n" + "req: " + req + "\n\n" + "routeResult: \n" + "target: "
                + routeResult.target() + "\n" + "pathParams: " + routeResult.pathParams() + "\n" + "queryParams: "
                + routeResult.queryParams() + "\n\n" + "allowedMethods: " + router.allowedMethods(req.uri());
        FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        res.headers().set(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
        return res;
    }

    private static void flushResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        if (!HttpUtil.isKeepAlive(req)) {
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        }
        else {
            res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(res);
        }
    }
}
