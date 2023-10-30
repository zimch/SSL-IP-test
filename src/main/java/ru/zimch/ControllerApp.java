package ru.zimch;

import io.javalin.Javalin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import ru.zimch.services.CertificateCheckService;
import ru.zimch.utils.FileUtil;

import java.util.Objects;
import java.util.function.Supplier;

public class ControllerApp {
    public static void main(String[] args) {
        Supplier<Server> serverSupplier = () -> {
            Server server = new Server(new QueuedThreadPool(10, 2, 60_000));

            return server;
        };

        Javalin app = Javalin.create(config -> {
            config.server(serverSupplier);
        }).start(8080);

        app.get("/", ctx -> {
            ctx.html(FileUtil.getFileContent("main.html"));
        });

        app.post("/find-domains", ctx -> {
            CertificateCheckService.findDomains(ctx.formParam("idmask"), Integer.parseInt(Objects.requireNonNull(ctx.formParam("threads"))));
            ctx.html("Domains are saving to file.");
        });

        app.exception(Exception.class, (exception, ctx) -> {
            System.out.println(exception.getMessage());
            ctx.html("Some errors :(");
        });

    }
}