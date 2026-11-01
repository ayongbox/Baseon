package io.github.ayongbox.baseon.core.launch;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;

/**
 * 启动横幅打印器：应用就绪后输出 ASCII 横幅与运行时信息。
 *
 * <p>刻意不走 {@code banner.txt}：classpath 根的 banner.txt 会覆盖业务自定义横幅，
 * 而 ApplicationRunner 方式与业务自有横幅互不冲突，且可用 {@code baseon.launch.banner-enabled}
 * 独立关闭。输出去 System.out（与 Spring Boot 原生横幅一致），不进日志文件。
 */
public class BaseonBannerPrinter implements ApplicationRunner {

    private static final String BANNER = "\n"
            + "  ____\n"
            + " |  _ \\\n"
            + " | |_) | __ _ ___  ___  ___  _ __\n"
            + " |  _ < / _` / __|/ _ \\/ _ \\| '_ \\\n"
            + " | |_) | (_| \\__ \\  __/ (_) | | | |\n"
            + " |____/ \\__,_|___/\\___|\\___/|_| |_|\n";

    private final Environment environment;

    public BaseonBannerPrinter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String applicationName = environment.getProperty("spring.application.name", "baseon-service");
        String bootVersion = SpringBootVersion.getVersion();
        StringBuilder output = new StringBuilder(BANNER)
                .append(" :: ").append(applicationName).append(" :: ")
                .append("Spring Boot ").append(bootVersion == null ? "unknown" : bootVersion)
                .append(" (JDK ").append(System.getProperty("java.version")).append(")");
        System.out.println(output);
    }
}
