package io.quarkus.cli.common;

import static org.apache.maven.cli.MavenCli.LOCAL_REPO_PROPERTY;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Assertions;

import io.quarkus.runtime.QuarkusApplication;

public class GenericCliDriver {

    public static final PrintStream stdout = System.out;
    public static final PrintStream stderr = System.err;
    static String MAVEN_SETTINGS = "maven.settings";

    static final BinaryOperator<String> ARG_FORMATTER = (key, value) -> "-D" + key + "=" + value;
    static final UnaryOperator<String> REPO_ARG_FORMATTER = value -> ARG_FORMATTER.apply(LOCAL_REPO_PROPERTY, value);
    static final UnaryOperator<String> SETTINGS_ARG_FORMATTER = value -> ARG_FORMATTER.apply(MAVEN_SETTINGS, value);

    public static class CliDriverBuilder {

        private Path startingDir;
        private List<String> args = new ArrayList<>();
        private String mavenLocalRepo;
        private String mavenSettings;
        private QuarkusApplication command;

        private CliDriverBuilder() {
        }

        public CliDriverBuilder setStartingDir(Path startingDir) {
            this.startingDir = startingDir;
            return this;
        }

        public CliDriverBuilder addArgs(String... args) {
            for (String s : args) {
                this.args.add(s);
            }
            return this;
        }

        public CliDriverBuilder setMavenRepoLocal(String mavenRepoLocal) {
            this.mavenLocalRepo = mavenRepoLocal;
            return this;
        }

        public CliDriverBuilder setMavenSettings(String mavenSettings) {
            this.mavenSettings = mavenSettings;
            return this;
        }

        public CliDriverBuilder setCommand(QuarkusApplication command) {
            this.command = command;
            return this;
        }

        public Result execute() throws Exception {
            List<String> newArgs = args;

            List<String> looseArgs = Collections.emptyList();
            int index = newArgs.indexOf("--");
            if (index >= 0) {
                looseArgs = new ArrayList<>(newArgs.subList(index, newArgs.size()));
                newArgs.subList(index, newArgs.size()).clear();
            }

            Optional.ofNullable(mavenLocalRepo).or(GenericCliDriver::getMavenLocalRepoProperty).map(REPO_ARG_FORMATTER)
                    .ifPresent(newArgs::add);
            Optional.ofNullable(mavenSettings).or(GenericCliDriver::getMavenSettingsProperty).map(SETTINGS_ARG_FORMATTER)
                    .ifPresent(newArgs::add);

            newArgs.add("--cli-test");
            newArgs.add("--cli-test-dir");
            newArgs.add(startingDir.toString());
            newArgs.addAll(looseArgs); // re-add arguments

            System.out.println("$ quarkus " + String.join(" ", newArgs));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream outPs = new PrintStream(out);
            System.setOut(outPs);

            ByteArrayOutputStream err = new ByteArrayOutputStream();
            PrintStream errPs = new PrintStream(err);
            System.setErr(errPs);

            final Map<String, String> originalProps = collectOverriddenProps(newArgs);

            Result result = new Result();
            try {
                result.exitCode = command.run(newArgs.toArray(String[]::new));
                outPs.flush();
                errPs.flush();
            } finally {
                System.setOut(stdout);
                System.setErr(stderr);
                resetProperties(originalProps);
            }
            result.stdout = out.toString();
            result.stderr = err.toString();
            return result;
        }

        protected void resetProperties(Map<String, String> originalProps) {
            for (Map.Entry<String, String> origProp : originalProps.entrySet()) {
                if (origProp.getValue() == null) {
                    System.clearProperty(origProp.getKey());
                } else {
                    System.setProperty(origProp.getKey(), origProp.getValue());
                }
            }
        }

        protected Map<String, String> collectOverriddenProps(List<String> newArgs) {
            final Map<String, String> originalProps = new HashMap<>();
            for (String s : newArgs) {
                if (s.startsWith("-D")) {
                    int equals = s.indexOf('=', 2);
                    if (equals > 0) {
                        final String propName = s.substring(2, equals);
                        final String origValue = System.getProperty(propName);
                        if (origValue != null) {
                            originalProps.put(propName, origValue);
                        } else if (System.getProperties().contains(propName)) {
                            originalProps.put(propName, "true");
                        } else {
                            originalProps.put(propName, null);
                        }
                    }
                }
            }
            return originalProps;
        }
    }

    public static CliDriverBuilder builder() {
        return new CliDriverBuilder();
    }

    public static void preserveLocalRepoSettings(Collection<String> args) {
        getMavenLocalRepoProperty().map(REPO_ARG_FORMATTER).ifPresent(args::add);
        getMavenSettingsProperty().map(SETTINGS_ARG_FORMATTER).ifPresent(args::add);
    }

    public static Result executeArbitraryCommand(Path startingDir, String... args) throws Exception {
        System.out.println("$ " + String.join(" ", args));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream outPs = new PrintStream(out);
        System.setOut(outPs);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream errPs = new PrintStream(err);
        System.setErr(errPs);

        Result result = new Result();
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(startingDir.toFile());
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            Process p = pb.start();
            p.waitFor();
            outPs.flush();
            errPs.flush();
        } finally {
            System.setOut(stdout);
            System.setErr(stderr);
        }
        result.stdout = out.toString();
        result.stderr = err.toString();
        return result;
    }

    public static Result execute(Path startingDir, QuarkusApplication command, String... args) throws Exception {
        return builder().setCommand(command).setStartingDir(startingDir).addArgs(args).execute();
    }

    public static void println(String msg) {
        System.out.println(msg);
    }

    public static class Result {
        int exitCode;
        String stdout;
        String stderr;

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public void setStdout(String stdout) {
            this.stdout = stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public void setStderr(String stderr) {
            this.stderr = stderr;
        }

        public void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        public void echoSystemOut() {
            System.out.println(stdout);
            System.out.println();
        }

        public void echoSystemErr() {
            System.out.println(stderr);
            System.out.println();
        }

        @Override
        public String toString() {
            return "result: {\n  exitCode: {" + exitCode
                    + "},\n  system_err: {" + stderr
                    + "},\n  system_out: {" + stdout + "}\n}";
        }
    }

    public static void deleteDir(Path path) throws Exception {
        if (!path.toFile().exists()) {
            return;
        }

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(f -> retryDelete(f));

        Assertions.assertFalse(path.toFile().exists());
    }

    public static String readFileAsString(Path path) throws Exception {
        return new String(Files.readAllBytes(path));
    }

    private static Optional<String> getMavenLocalRepoProperty() {
        return Optional.ofNullable(System.getProperty(LOCAL_REPO_PROPERTY));
    }

    private static Optional<String> getMavenSettingsProperty() {
        return Optional.ofNullable(System.getProperty(MAVEN_SETTINGS)).filter(value -> Files.exists(Path.of(value)));
    }

    private static void retryDelete(File file) {
        if (file.delete()) {
            return;
        }
        int i = 0;
        while (i++ < 10) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }
            if (file.delete()) {
                break;
            }
        }
    }
}
