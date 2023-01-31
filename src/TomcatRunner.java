import org.apache.catalina.*;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.cli.*;
import org.apache.tomcat.util.descriptor.web.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.util.*;

public class TomcatRunner {
    private static final String SERVLETS_PACKAGE = "servlets";
    private static final String FILTERS_PACKAGE = "filters";
    private static final int DEFAULT_TOMCAT_PORT = 8080;
    private static final int DEFAULT_DEFAULT_THREADS_COUNT = 5;
    private static final String DEFAULT_CONTEXT_PATH = "/";

    private final int port;
    private final int nThreads;
    private final String contextPath;
    private final Tomcat tomcat = new Tomcat();

    public TomcatRunner(int port, int nThreads, String contextPath) {
        this.port = port;
        this.nThreads = nThreads;
        this.contextPath = contextPath;
        init();
    }

    private void init() {
        StandardThreadExecutor executor = new StandardThreadExecutor();
        executor.setMaxThreads(nThreads);

        tomcat.setPort(port);

        String docBase = new File(".").getAbsolutePath();

        Context context = tomcat.addContext(contextPath, docBase);
        addServlets(tomcat, context);
        addFilters(context);
    }

    public void startServer() throws LifecycleException {
        tomcat.start();
        tomcat.getServer().await();
    }

    public List<Class<?>> findAllClasses(String packageName) {
        packageName = packageName.replaceAll("[.]", "/");
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        List<Class<?>> classes = new ArrayList<>();

        try (InputStream in = classLoader.getResourceAsStream(packageName);
             InputStreamReader isr = new InputStreamReader(in);
             BufferedReader reader = new BufferedReader(isr)) {

            addClasses(packageName, classes, reader);
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading package: " + packageName);
        }

        return classes;
    }

    private void addClasses(String packageName, List<Class<?>> classes, BufferedReader reader) throws IOException {
        for (String line; (line = reader.readLine()) != null;) {
            if (!line.endsWith(".class"))
                continue;

            line = line.substring(0, line.lastIndexOf('.'));
            String className = packageName + "." + line;

            try {
                Class<?> clazz = Class.forName(className);
                classes.add(clazz);
            } catch (ClassNotFoundException e) {
                throw  new IllegalStateException("Class not found: " + className);
            }
        }
    }

    private void addServlets(Tomcat tomcat, Context context) {
        List<Class<?>> servletClasses = findAllClasses(SERVLETS_PACKAGE);
        for (Class<?> servletClass : servletClasses) {
            WebServlet webServletAnn = servletClass.getAnnotation(WebServlet.class);
            if (webServletAnn == null)
                continue;

            String[] urlPatterns = webServletAnn.urlPatterns();
            if (urlPatterns.length == 0)
                continue;

            String servletName = servletClass.getSimpleName();
            tomcat.addServlet(contextPath, servletName, servletClass.toString());

            for (String urlPattern : urlPatterns)
                context.addServletMappingDecoded(urlPattern, servletName);
        }
    }

    private void addFilters(Context context) {
         List<Class<?>> filterClasses = findAllClasses(FILTERS_PACKAGE);
         for (Class<?> filterClass : filterClasses) {
             WebFilter webFilterAnn = filterClass.getAnnotation(WebFilter.class);
             if (webFilterAnn == null)
                 continue;

             String[] servletNames = webFilterAnn.servletNames();
             if (servletNames.length == 0)
                 continue;

             addFilterDef(context, filterClass);

             for (String servletName : servletNames)
                 addFilterMap(context, filterClass, servletName);
         }
    }

    private void addFilterDef(Context context, Class<?> filterClass) {
        FilterDef filter = new FilterDef();
        filter.setFilterName(filterClass.getSimpleName());
        filter.setFilterClass(filterClass.getName());
        context.addFilterDef(filter);
    }

    private void addFilterMap(Context context, Class<?> filterClass, String servletName) {
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName(filterClass.getSimpleName());
        filterMap.addServletName(servletName);
        context.addFilterMap(filterMap);
    }

    public static void main(String[] args) throws LifecycleException {
        CommandLine cmd = getCommandLine(args);
        if (cmd == null)
            return;

        String optionStr = cmd.getOptionValue("p");
        int port = optionStr != null ? Integer.parseInt(optionStr) : DEFAULT_TOMCAT_PORT;

        optionStr = cmd.getOptionValue("t");
        int nThreads = optionStr != null ? Integer.parseInt(optionStr) : DEFAULT_DEFAULT_THREADS_COUNT;

        String contextPath = cmd.getOptionValue("c", DEFAULT_CONTEXT_PATH);

        TomcatRunner tomcatStarter = new TomcatRunner(port, nThreads, contextPath);
        tomcatStarter.startServer();
    }

    private static CommandLine getCommandLine(String[] args) {
        if (args == null) {
            printUsage();
            return null;
        }

        Options options = createOptions();
        CommandLine cmd = parseOptions(args, options);

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("tomcat-runner", options);
            return null;
        }

        String[] arguments = cmd.getArgs();

        if (arguments.length != 0) {
            printUsage();
            return null;
        }

        return cmd;
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption("p", "port", true, "порт");
        options.addOption("t", "threads", true, "брой нишки");
        options.addOption("c", "context_path", true, "базов url");
        options.addOption("h", "help", false, "показва описание на опциите");
        return options;
    }

    private static CommandLine parseOptions(String[] args, Options options) {
        try {
            CommandLineParser parser = new DefaultParser();
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("tomcat-runner: unknown option --");
        }
        return null;
    }

    private static void printUsage() {
        System.out.println("Usage: tomcat-runner [options]");
    }
}
