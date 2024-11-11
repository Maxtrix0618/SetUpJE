import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

/**
 * SetUpEXE: jar -> exe
 * by Paperfish, 2024.11.11
 */
public class SetUpFrame extends JFrame {
    private static final Font Font1 = new Font("Microsoft YaHei", Font.PLAIN, 14);
    private static final Font Font2 = new Font("Microsoft YaHei", Font.PLAIN, 12);
    private static final Color RED = new Color(218, 43, 43);
    private static final Color GREEN = new Color(18, 189, 7, 255);
    private static final Color WHITE = new Color(230, 230, 230);
    private static final Color D_GRAY = Color.DARK_GRAY;
    private static final Color GRAY = Color.GRAY;
    private static final String Name = "程序名称：";
    private static final String ProjectFolder = "项目目录：";
    private static final String JarPath = "jar文件位置：";
    private static final String JrePath = "jre目录位置：";
    private static final String OutputFolder = "输出到：";
    private static final String IconPath = "图标：";
    private static final String Version = "版本号：";
    private static final String Vendor = "供应商：";
    private static final String Copyright = "版权信息：";
    private static final String Description = "应用描述：";
    private final StringBuilder help = new StringBuilder();
    private final Map<String, JTextField> IF = new HashMap<>();
    private final Map<String, JLabel> IT = new HashMap<>();
    private final JTextArea cmdBack = new JTextArea();
    private JButton gJREb = new JButton();
    private int Y = 20;

    public SetUpFrame() {
        setTitle("SetUpEXE: jar -> exe");
        setSize(480, 640);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(null);
        addComponents();
        setVisible(true);
        preconfigure();
    }

    /**
     * Initial configuration of the program, including the use of windows style pop-ups,
     * the use of default path configuration, and help messages
     */
    private void preconfigure() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.focus", new ColorUIResource(new Color(0, 0, 0, 0)));
            Files.readAllLines(Path.of("./resource/help.txt")).forEach((e) -> help.append(e).append("\n"));
            List<String> defaultPaths = Files.readAllLines(Path.of("./resource/default.txt"));
            fill(ProjectFolder, defaultPaths.get(0));
            fill(OutputFolder, defaultPaths.get(1));
        } catch (Exception ignored) {}
    }

    /**
     * Add the required components to the window
     */
    private void addComponents() {
        addLabel(Name, 20, Y, 100, 180);
        addText(".exe", 305, Y, 40, D_GRAY);
        addHelpButton();
        addLine("预设", Y += 30);
        addLabel(ProjectFolder, 20, Y += 25, 100, 280);
        addLine("", Y += 20);
        addLabel(JarPath, 20, Y += 30, 100, 280);
        addLabel(JrePath, 20, Y += 30, 100, 280);
        addClearCacheButton();
        addLine("选填", Y += 50);
        addLabel(IconPath, 20, Y += 25, 100, 280);
        addLabel(Version, 20, Y += 30, 100, 80);
        addLabel(Vendor, 220, Y, 60, 120);
        addLabel(Copyright, 20, Y += 30, 100, 280);
        addLabel(Description, 20, Y += 30, 100, 280);
        addLine("", Y += 20);
        addLabel(OutputFolder, 20, Y += 30, 100, 280);
        addExecuteButton();
        addLine("命令行", Y += 30);
        addCommandTextArea();
        addFolderButton(ProjectFolder, "");
        addFolderButton(JarPath, ".jar");
        addFolderButton(JrePath, "");
        addFolderButton(IconPath, ".ico");
        addFolderButton(OutputFolder, "");
        addGenerateJreButton();
    }

    private void addLabel(String text, int x, int y, int tw, int fw) {
        addText(text, x, y, tw, D_GRAY);
        JTextField field = new JTextField();
        field.setFont(Font2);
        field.setLocation(x + tw, y);
        field.setSize(fw, 24);
        field.setBorder(BorderFactory.createBevelBorder(1));
        IF.put(text, field);
        add(field);
    }
    private void addLine(String item, int y) {
        addText("——" + item + "—".repeat(26 - item.length()), 15, y, 460, GRAY);
    }
    private void addText(String text, int x, int y, int tw, Color foreground) {
        JLabel label = new JLabel(text);
        label.setLocation(x, y);
        label.setSize(tw, 24);
        label.setForeground(foreground);
        label.setFont(Font1);
        IT.put(text, label);
        add(label);
    }

    /**
     * The button to show Help message.
     */
    private void addHelpButton() {
        JButton button = newButton("Help", 370, Y, 70, 24, Font2);
        button.addActionListener((e) -> JOptionPane.showMessageDialog(this, help, "帮助", JOptionPane.INFORMATION_MESSAGE));
        add(button);
    }

    private void addClearCacheButton() {
        JButton button = newButton("ClearCF", 20, Y+28, 80, 16, Font2);
        button.addActionListener((e) -> clearCache());
        add(button);
    }
    private void addExecuteButton() {
        JButton button = newButton("确认构建程序", 20, Y += 40, 420, 24, Font1);
        button.addActionListener((e) -> buildEXE());
        add(button);
    }

    /**
     * The button used to generate the required jre package from the jar artifact.<br/>
     * The program executes the 'jdeps' command to obtain the required list of Java modules,
     * then uses the 'jlink' command to generate a custom jre containing these modules,
     * and finally automatically fills their paths into the jre project bar.
     */
    private void addGenerateJreButton() {
        JTextField jreField = IF.get(JrePath);
        jreField.setVisible(false);
        gJREb = newButton("点击根据Jar生成", jreField.getX(), jreField.getY(), jreField.getWidth(), jreField.getHeight(), Font2);
        gJREb.addActionListener((e) -> {
            if (!denyText(ProjectFolder)) {
                String tryJreFolder = IF.get(ProjectFolder).getText().trim() + "\\jre";
                if (!denyText(JarPath)) {
                    if (isValidJRE(tryJreFolder) && JOptionPane.showConfirmDialog(this, "项目下已有Jre，是否覆盖（推荐）？", "Confirm", JOptionPane.OK_CANCEL_OPTION) != 0) {
                        fill(JrePath, tryJreFolder);
                        IF.get(JrePath).setVisible(true);
                        gJREb.setVisible(false);
                        return;
                    }
                    deleteFolder(tryJreFolder);
                    String[] jm_back = CMD("jdeps --print-module-deps " + IF.get(JarPath).getText().trim());
                    if (jm_back[0].isBlank() || !jm_back[1].equals("0")) {
                        JOptionPane.showMessageDialog(this, "无法解析提供的Jar文件", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String[] jre_back = CMD("jlink --no-header-files --no-man-pages --add-modules " + jm_back[0] + " --output " + tryJreFolder);
                    if (!jre_back[1].equals("0")) {
                        JOptionPane.showMessageDialog(this, "未能生成Jre", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    JOptionPane.showMessageDialog(this, "已更新Jre", "Done!", JOptionPane.INFORMATION_MESSAGE);
                    fill(JrePath, tryJreFolder);
                    IF.get(JrePath).setVisible(true);
                    gJREb.setVisible(false);

                } else {JOptionPane.showMessageDialog(this, "Jar文件不可用！", "Error", JOptionPane.ERROR_MESSAGE);}
            } else {JOptionPane.showMessageDialog(this, "请指定IDEA项目目录", "Error", JOptionPane.ERROR_MESSAGE);}
        });
        add(gJREb);
    }

    /**
     * The text box used to display the output of windows cmd.exe
     */
    private void addCommandTextArea() {
        cmdBack.setLineWrap(true);
        cmdBack.setEditable(false);
        cmdBack.setBackground(WHITE);
        cmdBack.setFont(Font2);
        JScrollPane textJSP = new JScrollPane(cmdBack);
        textJSP.setLocation(20, Y += 30);
        textJSP.setSize(420, 120);
        add(textJSP);
    }

    /**
     * Add a find button to the project so that the user can find the file through the file selector.
     */
    private void addFolderButton(String item, String type) {
        JTextField textField = IF.get(item);
        JButton button = newButton("+", 400, textField.getY(), 40, 24, Font2);
        button.addActionListener((e) -> {
            JFileChooser chooser = new JFileChooser(textField.getText());
            chooser.setFileSelectionMode(1);
            if (chooser.showOpenDialog(this) == 0) {
                String path;
                if (!(path = validPath(chooser.getSelectedFile().getAbsolutePath(), type)).isEmpty()) {
                    fill(item, path);
                    if (ProjectFolder.equals(item)) {
                        completeSomePaths();
                    } else if (JrePath.equals(item)) {
                        textField.setVisible(true);
                        gJREb.setVisible(false);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "文件类型错误，应为" + type, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        add(button);
    }

    /**
     * Fill the item with the given text.
     */
    private void fill(String item, String text) {
        IF.get(item).setText(text);
        IT.get(item).setForeground(D_GRAY);
    }

    /**
     * Check if there is already text there before fill(), if so, do not fill.
     */
    private void tryFill(String item, String text) {
        if (IF.get(item).getText().isEmpty()) fill(item, text);
    }

    /**
     * Automatically locates the path to some specific required files based on the entered project path.
     */
    private void completeSomePaths() {
        String pf = IF.get(ProjectFolder).getText();
        if (!pf.isBlank()) {
            tryFill(Name, pf.substring(pf.lastIndexOf(92) + 1));
            tryFill(JarPath, firstFileIn(pf + "\\out\\artifacts", ".jar", 16));
            tryFill(IconPath, firstFileIn(pf + "\\resource", ".ico", 16));
            tryFill(OutputFolder, pf + "\\out");
        }
    }

    /**
     * Build exe program.
     */
    private void buildEXE() {
        if (denyText(Name) | denyText(JarPath) | denyText(JrePath) | denyText(OutputFolder) | denyText(IconPath)) {
            JOptionPane.showMessageDialog(this, "请检查标红的条目", "Error", JOptionPane.ERROR_MESSAGE);
        } else if (JOptionPane.showConfirmDialog(this, "确认开始构建程序？", "Confirm", JOptionPane.OK_CANCEL_OPTION) == 0) {
            String tryTarget = IF.get(OutputFolder).getText().trim() + "\\" + IF.get(Name).getText().trim();
            if (validPath(tryTarget, "").isEmpty() || JOptionPane.showConfirmDialog(this,
                    "输出目录下存在同名程序包，是否覆盖？", "Confirm", JOptionPane.OK_CANCEL_OPTION) == 0) {
                deleteFolder(tryTarget);
                File jarFile = new File(IF.get(JarPath).getText().trim());
                String jar = jarFile.getName();
                String jarFolder = jarFile.getParent();

                StringBuilder command = new StringBuilder("jpackage --type app-image");
                command.append(" --input \"").append(jarFolder)
                        .append("\" --main-jar \"").append(jar)
                        .append("\" --runtime-image \"").append(IF.get(JrePath).getText().trim())
                        .append("\" --dest \"").append(IF.get(OutputFolder).getText().trim())
                        .append("\" --name \"").append(IF.get(Name).getText().trim()).append("\"");
                tryAppend(command, " --icon ", IconPath);
                tryAppend(command, " --app-version ", Version);
                tryAppend(command, " --vendor ", Vendor);
                tryAppend(command, " --copyright ", Copyright);
                tryAppend(command, " --description ", Description);
                String[] exe_back = CMD(command.toString());
                if (!exe_back[1].equals("0")) {
                    JOptionPane.showMessageDialog(this, "构建失败，请检查命令行", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "成功构建exe到输出目录", "Done!", JOptionPane.INFORMATION_MESSAGE);
                }

            }
        }
    }

    /**
     * Determine whether the entered field is unavailable.
     */
    private boolean denyText(String item) {
        String text = IF.get(item).getText().trim();
        Color pass = RED;
        switch(item) {
            case ProjectFolder -> {if (!firstFileIn(text, ".iml", 1).isEmpty()) pass = D_GRAY;}
            case Name, OutputFolder -> {if (!text.isEmpty()) pass = GREEN;}
            case JarPath -> {if (!validPath(text, ".jar").isEmpty()) pass = GREEN;}
            case JrePath -> {if (isValidJRE(text)) pass = GREEN;}
            case IconPath -> {if (text.isEmpty()) pass = D_GRAY; else if (!validPath(text, ".ico").isEmpty()) pass = GREEN;}
        }
        IT.get(item).setForeground(pass);
        return pass.equals(RED);
    }

    /**
     * Try to add optional items to the command, if not, do not add
     */
    private void tryAppend(StringBuilder command, String guidance, String item) {
        String text;
        if (!(text = IF.get(item).getText().trim()).isEmpty()) {
            command.append(guidance).append("\"").append(text).append("\"");
        }
    }

    /**
     * Determines whether the path points to a valid file of a given type.
     */
    private static String validPath(String tryPath, String type) {
        return (new File(tryPath)).exists() && tryPath.endsWith(type) ? tryPath : "";
    }

    /**
     * Returns the path to the first file of the given type found in that directory.
     * @param tryPath the path of directory
     * @param depth The depth of the query (the maximum level of subdirectories under the directory to be queried)
     */
    private static String firstFileIn(String tryPath, String type, int depth) {
        try {
            return Files.find(Paths.get(tryPath), depth, (p, a) -> p.toString().toLowerCase().endsWith(type), new FileVisitOption[0])
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .findFirst().orElse("");
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Determine whether the given path is a valid jre (whether it contains lib and bin, and a java.exe in the bin).
     */
    private static boolean isValidJRE(String jrePath) {
        File jreDir = new File(jrePath);
        if (jreDir.exists() && jreDir.isDirectory()) {
            File binDir = new File(jreDir, "bin");
            File javaExecutable = new File(binDir, "java" + (isWindows() ? ".exe" : ""));
            if (binDir.exists() && binDir.isDirectory() && javaExecutable.exists()) {
                File libDir = new File(jreDir, "lib");
                return libDir.exists() && libDir.isDirectory();
            } else {return false;}
        } else {return false;}
    }
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Run the entered command in windows cmd.exe
     */
    private String[] CMD(String command) {
        cmdBack.append(command);
        String endCode = "";
        StringBuilder result = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            endCode = String.valueOf(process.waitFor());
            cmdBack.append(result.toString());
            cmdBack.append("\n" + endCode + "\n\n");
        } catch (InterruptedException | IOException ex) {
            cmdBack.append(ex.getMessage());
        }
        return new String[]{result.toString(), endCode};
    }

    /**
     * Clear cached files, including Jar artifacts and Jre packages.
     */
    private void clearCache() {
        boolean deleteJRE = false;
        boolean deleteJAR = false;
        StringBuilder caution = new StringBuilder("根据路径找到了以下缓存文件，确定删除？");
        String jrePath = IF.get(JrePath).getText().trim();
        String jarPath = IF.get(JarPath).getText().trim();
        if (isValidJRE(jrePath)) {caution.append("\n").append(jrePath); deleteJRE = true;}
        if (!validPath(jarPath, ".jar").isEmpty()) {caution.append("\n").append(jarPath); deleteJAR = true;}
        if (!deleteJRE && !deleteJAR) {
            JOptionPane.showMessageDialog(this, "没有需要清除的缓存文件", "ClearCF", JOptionPane.INFORMATION_MESSAGE);
        } else if (JOptionPane.showConfirmDialog(this, caution, "Confirm", JOptionPane.OK_CANCEL_OPTION) == 0) {
            if (deleteJRE) deleteFolder(jrePath);
            if (deleteJAR) deleteFolder(jarPath);
            JOptionPane.showMessageDialog(this, "已清除缓存文件", "ClearCF", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Delete the folder pointed to by the path and all sub-folders and files under it.<br/>
     * Please use this method carefully! Be sure to check that the file path you want to delete is correct.
     */
    private void deleteFolder(String folderPath) {
        if (!folderPath.isBlank() && Files.exists(Path.of(folderPath))) {
            try (Stream<Path> paths = Files.walk(Path.of(folderPath))) {
                List<String> error = new ArrayList<>();
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            if (!path.toFile().delete()) error.add(String.valueOf(paths));
                        });
                if (!error.isEmpty()) {
                    StringBuilder str = new StringBuilder("未能删除以下文件：");
                    error.forEach(e -> str.append(e).append("\n"));
                    JOptionPane.showMessageDialog(this, str, "Warning", JOptionPane.WARNING_MESSAGE);
                }
            } catch (IOException ignored) {}
        }
    }

    /**
     * Returns a unified configuration new button
     */
    private static JButton newButton(String text, int x, int y, int w, int h, Font font) {
        JButton button = new JButton(text);
        button.setLocation(x, y);
        button.setSize(w, h);
        button.setFont(font);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        return button;
    }

    public static void main(String[] args) {
        new SetUpFrame();
    }

}
