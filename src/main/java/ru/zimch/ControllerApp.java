package ru.zimch;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import ru.zimch.services.CertificateCheckService;

import java.util.Objects;
import java.util.function.Supplier;

public class ControllerApp {
    public static void main(String[] args) {
        Supplier<Server> serverSupplier = () -> {
            Server server = new Server(new QueuedThreadPool(10, 2, 60_000));

            return server;
        };

        Javalin app = Javalin.create(config -> {
            config.jetty.server(serverSupplier);
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(8080);

        app.post("/find-domains", ctx -> {
            CertificateCheckService.findDomains(ctx.formParam("idmask"), Integer.parseInt(Objects.requireNonNull(ctx.formParam("threads"))));
            ctx.html("Domains are saving to file.");
        });

    }
}