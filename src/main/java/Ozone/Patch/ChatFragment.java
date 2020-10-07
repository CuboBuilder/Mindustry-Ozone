package Ozone.Patch;


import Atom.Time.Countdown;
import Atom.Utility.Utility;
import Garbage.Settings;
import Ozone.Commands.Commands;
import arc.Core;
import arc.Input;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.math.Mathf;
import arc.scene.Group;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.input.Binding;
import mindustry.ui.Fonts;
import mindustry.ui.fragments.Fragment;

import java.util.HashMap;

import static arc.Core.input;
import static arc.Core.scene;
import static mindustry.Vars.*;

public class ChatFragment extends mindustry.ui.fragments.ChatFragment {

    private static final int messagesShown = 10;
    private Seq<ChatMessage> messages = new Seq<>();
    private float fadetime;
    private boolean shown = false;
    private TextField chatfield;
    private Label fieldlabel = new Label(">");
    private Font font;
    private GlyphLayout layout = new GlyphLayout();
    private float offsetx = Scl.scl(4), offsety = Scl.scl(4), fontoffsetx = Scl.scl(2), chatspace = Scl.scl(50);
    private Color shadowColor = new Color(0, 0, 0, 0.4f);
    private float textspacing = Scl.scl(10);
    private Seq<String> history = new Seq<>();
    private int historyPos = 0;
    private int scrollPos = 0;
    private Fragment container = new Fragment() {
        public void build(Group parent) {
            Core.scene.add(ChatFragment.this);
        }
    };
    private HashMap<String, AntiSpam> antiSpam = new HashMap<>();

    public ChatFragment() {
        super();

        setFillParent(true);
        font = Fonts.def;

        visible(() -> {
            if (!net.active() && messages.size > 0) {
                clearMessages();

                if (shown) {
                    hide();
                }
            }

            return net.active();
        });

        update(() -> {

            if (net.active() && input.keyTap(Binding.chat) && (scene.getKeyboardFocus() == chatfield || scene.getKeyboardFocus() == null || ui.minimapfrag.shown()) && !ui.scriptfrag.shown()) {
                toggle();
            }

            if (shown) {
                if (input.keyTap(Binding.chat_history_prev) && historyPos < history.size - 1) {
                    if (historyPos == 0) history.set(0, chatfield.getText());
                    historyPos++;
                    updateChat();
                }
                if (input.keyTap(Binding.chat_history_next) && historyPos > 0) {
                    historyPos--;
                    updateChat();
                }
                scrollPos = (int) Mathf.clamp(scrollPos + input.axis(Binding.chat_scroll), 0, Math.max(0, messages.size - messagesShown));
            }
        });

        history.insert(0, "");
        setup();
    }

    @Override
    public Fragment container() {
        return container;
    }


    @Override
    public void clearMessages() {
        antiSpam.clear();
        this.messages.clear();
        this.history.clear();
        this.history.insert(0, "");
    }

    private void setup() {
        fieldlabel.setStyle(new Label.LabelStyle(fieldlabel.getStyle()));
        fieldlabel.getStyle().font = font;
        fieldlabel.setStyle(fieldlabel.getStyle());

        chatfield = new TextField("", new TextField.TextFieldStyle(scene.getStyle(TextField.TextFieldStyle.class)));
        chatfield.setMaxLength(Vars.maxTextLength);
        chatfield.getStyle().background = null;
        chatfield.getStyle().font = Fonts.chat;
        chatfield.getStyle().fontColor = Color.white;
        chatfield.setStyle(chatfield.getStyle());

        bottom().left().marginBottom(offsety).marginLeft(offsetx * 2).add(fieldlabel).padBottom(6f);

        add(chatfield).padBottom(offsety).padLeft(offsetx).growX().padRight(offsetx).height(28);

        if (Vars.mobile) {
            marginBottom(105f);
            marginRight(240f);
        }
    }

    @Override
    public void draw() {
        float opacity = Core.settings.getInt("chatopacity") / 100f;
        float textWidth = Math.min(Core.graphics.getWidth() / 1.5f, Scl.scl(700f));

        Draw.color(shadowColor);

        if (shown) {
            Fill.crect(offsetx, chatfield.y, chatfield.getWidth() + 15f, chatfield.getHeight() - 1);
        }

        super.draw();

        float spacing = chatspace;

        chatfield.visible = shown;
        fieldlabel.visible = shown;

        Draw.color(shadowColor);
        Draw.alpha(shadowColor.a * opacity);

        float theight = offsety + spacing + getMarginBottom();
        for (int i = scrollPos; i < messages.size && i < messagesShown + scrollPos && (i < fadetime || shown); i++) {

            layout.setText(font, messages.get(i).formattedMessage, Color.white, textWidth, Align.bottomLeft, true);
            theight += layout.height + textspacing;
            if (i - scrollPos == 0) theight -= textspacing + 1;

            font.getCache().clear();
            font.getCache().addText(messages.get(i).formattedMessage, fontoffsetx + offsetx, offsety + theight, textWidth, Align.bottomLeft, true);

            if (!shown && fadetime - i < 1f && fadetime - i >= 0f) {
                font.getCache().setAlphas((fadetime - i) * opacity);
                Draw.color(0, 0, 0, shadowColor.a * (fadetime - i) * opacity);
            } else {
                font.getCache().setAlphas(opacity);
            }

            Fill.crect(offsetx, theight - layout.height - 2, textWidth + Scl.scl(4f), layout.height + textspacing);
            Draw.color(shadowColor);
            Draw.alpha(opacity * shadowColor.a);

            font.getCache().draw();
        }

        Draw.color();

        if (fadetime > 0 && !shown)
            fadetime -= Time.delta / 180f;
    }

    private void sendMessage() {
        String message = this.chatfield.getText();
        this.clearChatInput();
        if (!message.replace(" ", "").isEmpty()) {
            if (Commands.call(message)) return;
            this.history.insert(1, message);
            Call.sendChatMessage(message);
        }
    }

    @Override
    public void toggle() {

        if (!shown) {
            scene.setKeyboardFocus(chatfield);
            shown = !shown;
            if (mobile) {
                Input.TextInput input = new Input.TextInput();
                input.maxLength = maxTextLength;
                input.accepted = text -> {
                    chatfield.setText(text);
                    sendMessage();
                    hide();
                    Core.input.setOnscreenKeyboardVisible(false);
                };
                input.canceled = this::hide;
                Core.input.getTextInput(input);
            } else {
                chatfield.fireClick();
            }
        } else {
            scene.setKeyboardFocus(null);
            shown = !shown;
            scrollPos = 0;
            sendMessage();
        }
    }


    @Override
    public void hide() {
        scene.setKeyboardFocus(null);
        shown = false;
        clearChatInput();
    }

    @Override
    public void updateChat() {
        chatfield.setText(history.get(historyPos));
        chatfield.setCursorPosition(chatfield.getText().length());
    }

    @Override
    public void clearChatInput() {
        historyPos = 0;
        history.set(0, "");
        chatfield.setText("");
    }

    @Override
    public boolean shown() {
        return shown;
    }


    @Override
    public void addMessage(String message, String sender) {
        ChatMessage cm = new ChatMessage(message, sender);
        if (Settings.antiSpam) {
            if (antiSpam.containsKey(sender)) {
                AntiSpam victim = antiSpam.get(sender);
                ChatMessage message1 = messages.get(0);
                String filter = victim.filter(message);
                if (!filter.isEmpty()) {
                    if (message1.message.equals(victim.lastMessage)) messages.remove(0);
                    messages.insert(0, new ChatMessage(filter, sender, victim.reasons));
                } else messages.insert(0, cm);
            } else {
                antiSpam.put(sender, new AntiSpam(sender));
                messages.insert(0, cm);
                fadetime += 1f;
                fadetime = Math.min(fadetime, messagesShown) + 1f;
                if (scrollPos > 0) scrollPos++;
            }
            antiSpam.get(sender).setLastMessage(cm.message);
        } else {
            messages.insert(0, cm);

            fadetime += 1f;
            fadetime = Math.min(fadetime, messagesShown) + 1f;

            if (scrollPos > 0) scrollPos++;
        }
    }

    private static class AntiSpam {
        public static int maxRepeatingChar = 100;
        public static long rateLimit = 300;
        public final String sender;
        public String lastMessage = "";
        public int lastMessageTimes = 1;
        public String reasons = "";
        private long lastMessageSent = 0;

        public AntiSpam(String name) {
            sender = name;
        }

        public void setLastMessage(String message) {
            if (message.equalsIgnoreCase(lastMessage)) lastMessageTimes++;
            else {
                lastMessageTimes = 1;
                lastMessage = message;
            }
            lastMessageSent = System.currentTimeMillis();
        }

        public String filter(String message) {
            if ((message.length() - Utility.shrinkString(message).length()) > maxRepeatingChar) {
                reasons = "Duplicate String from: " + message.length() + " to " + Utility.shrinkString(message).length();
            }
            if (message.equalsIgnoreCase(lastMessage)) {
                reasons = "Spam Last Message: " + lastMessageTimes;
            } else if ((System.currentTimeMillis() - lastMessageSent) > rateLimit) {
                reasons = "Too Fast: " + Countdown.result(lastMessageSent);
            } else reasons = "";

            return message;
        }
    }

    private static class ChatMessage {
        public final String sender;
        public final String message;
        public final String formattedMessage;

        public ChatMessage(String message, String sender) {
            this.message = message;
            this.sender = sender;
            if (sender == null) {
                this.formattedMessage = message;
            } else {
                this.formattedMessage = "[coral][[" + sender + "[coral]]:[white] " + message;
            }

        }

        public ChatMessage(String message, String sender, String antiSpam) {
            this.message = message;
            this.sender = sender;
            if (sender == null) {
                this.formattedMessage = message;
            } else {
                this.formattedMessage = "[royal][AntiSpam][white]" + antiSpam + "\n[coral][[" + sender + "[coral]]:[white] " + message;
            }

        }
    }
}
