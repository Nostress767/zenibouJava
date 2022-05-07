package com.sun.jna.platform.win32;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Native;

import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.Win32VK;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

class Example {
    public static void main(String[] args){
        Zenibou window = new Zenibou(500, 500, "サンプル");

        while(window.isRunning){
            window.beginFrame();
                window.c(0xFFFF00);

                if(window.key['P'].isPressed())
                    window.closeWindow();

                window.c(0xFFFF00);
                for(int i = 0; i < 100; i++)
                    for(int j = 0; j < 100; j++)
                        window.d(100+j, 100+i, 0x00FF00);

            window.endFrame();
        }
    }
}

// TODO: Figure out how to place this in another file
class Zenibou implements WinUser.WindowProc{
    Key[] key;

    int width, height;
    int currentPosX, currentPosY;
    boolean isRunning;
    boolean isFocused;

    private WinDef.HWND handle;
    private WinUser.MSG msg;
    private WinDef.HMODULE instance;

    private WinDef.HDC bitmap_device_context;
    private WinGDI.BITMAPINFOHEADER bitmap_info_header;
    private WinGDI.BITMAPINFO bitmap_info;
    private Memory bitmap_memory;

    public Zenibou(int size_x, int size_y, String title) {
        key = new Key[512];
        for(int i = 0; i < key.length; i++)
            key[i] = new Key();
        instance = Kernel32.INSTANCE.GetModuleHandle("");

        String className = "ZenibouJava";
        WinUser.WNDCLASSEX wClass = new WinUser.WNDCLASSEX();
        wClass.hInstance = instance;
        wClass.lpfnWndProc = Zenibou.this;
        wClass.lpszClassName = className;
        wClass.style = USR.INST.CS_HREDRAW | USR.INST.CS_VREDRAW;
        
        wClass.hCursor = USR.INST.LoadCursorW((WinDef.HINSTANCE)null, WinUser.IDC_ARROW);
        
        USR.INST.SetProcessDPIAware();
        USR.INST.ShowCursor(new WinDef.BOOL(true));

        currentPosX = (User32.INSTANCE.GetSystemMetrics(WinUser.SM_CXSCREEN) / 2) - (size_x / 2);
        currentPosY = (User32.INSTANCE.GetSystemMetrics(WinUser.SM_CYSCREEN) / 2) - (size_y / 2); 

        User32.INSTANCE.RegisterClassEx(wClass);

        WinDef.RECT rect = new WinDef.RECT();
        rect.right = size_x;
        rect.bottom = size_y;
        WinDef.DWORD window_style = new WinDef.DWORD(WinUser.WS_OVERLAPPEDWINDOW | WinUser.WS_VISIBLE);
        User32.INSTANCE.AdjustWindowRectEx(rect, window_style, new WinDef.BOOL(false), (WinDef.DWORD)null);

        // TODO: fix scaling and resizing
        handle = User32.INSTANCE.CreateWindowEx(//User32.WS_EX_TOPMOST,//User32.WS_EX_OVERLAPPEDWINDOW, //(https://docs.microsoft.com/en-us/windows/win32/winmsg/extended-window-styles)
                        0,
                        className, title,
                        WinUser.WS_OVERLAPPEDWINDOW | WinUser.WS_VISIBLE,
                        currentPosX, currentPosY,
                        rect.right - rect.left, rect.bottom - rect.top,
                        null, null,
                        instance, null);

        // TODO: Make clock
        //InitializeClock();
        isRunning = true;
        isFocused = true;
        width = size_x;
        height = size_y;
        
        bitmap_memory = new Memory(size_x * size_y * 4);
        bitmap_memory.clear(); // Zero-Initialize memory

        bitmap_info_header = new WinGDI.BITMAPINFOHEADER();
        bitmap_info_header.biWidth = size_x;
        bitmap_info_header.biHeight = size_y;
        bitmap_info_header.biPlanes = 1;
        bitmap_info_header.biBitCount = 32;
        bitmap_info_header.biCompression = WinGDI.BI_RGB;
        bitmap_info = new WinGDI.BITMAPINFO();
        bitmap_info.bmiHeader = bitmap_info_header;

        bitmap_device_context = User32.INSTANCE.GetDC(handle);

        msg = new WinUser.MSG();
    }

    @Override
    public WinDef.LRESULT callback(WinDef.HWND hwnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
        switch(uMsg){
            case WinUser.WM_KEYUP:
            case WinUser.WM_SYSKEYDOWN:
            case WinUser.WM_SYSKEYUP:
            case WinUser.WM_KEYDOWN:{
                UpdateKeyState(wParam.intValue(), lParam.intValue());
            } return new WinDef.LRESULT(0);
            case WinUser.WM_DESTROY: {
                isRunning = false;
                User32.INSTANCE.PostQuitMessage(0);
            } break;
        }
        return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
    }
    protected void c(int color) {
        for(int i = 0; i < height; i++)
            for(int j = 0; j < width; j++)
                d(j, i, color);
    }
    protected void d(int x, int y, int color) {
        if(x < 0 || y < 0 || x >= width || y >= height)
            return;
        // On zenibou its ARGB
        bitmap_memory.write((y * width + x) * 4, new int[]{color}, 0, 1);
    }
    public void closeWindow(){
        User32.INSTANCE.DestroyWindow(handle);
    }
    protected void beginFrame() {
        while (User32.INSTANCE.PeekMessage(msg, null, 0, 0, USR.INST.PM_REMOVE)) {
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }        
    }
    protected void endFrame() {
        GDI.INST.StretchDIBits(bitmap_device_context,
                        0,0,
                        width, height,
                        0, 0,
                        width, height,
                        bitmap_memory,
                        bitmap_info,
                        WinGDI.DIB_RGB_COLORS,
                        GDI32.SRCCOPY);

        for(int i = 0; i < 512; i++){
            if(key[i].isPressed()){
                key[i].setPressed(false);
                key[i].setHeld(true);
                key[i].setReleased(false);}
            else if(key[i].isHeld()){
            }
            else if(key[i].isReleased()){
                key[i].setPressed(false);
                key[i].setHeld(false);
                key[i].setReleased(false);
            }
        }
    }
    protected void UpdateKeyState(int key_type, int bitfield) {
        int mapped_key = key_type;

        boolean was_down = ((bitfield >>> 30) & 1) == 1;
        boolean is_down = ((bitfield >>> 31) ^ 1) == 1;
        switch (Win32VK.fromValue(key_type)) {
            case VK_SPACE:      mapped_key = Key.Space;        break;
            case VK_OEM_7:      mapped_key = Key.Quotes;       break;
            case VK_OEM_COMMA:  mapped_key = Key.Comma;        break;
            case VK_OEM_MINUS:  mapped_key = Key.Minus;        break;
            case VK_OEM_PERIOD: mapped_key = Key.Period;       break;
            case VK_OEM_2:      mapped_key = Key.FrontSlash;   break;
            case VK_OEM_1:      mapped_key = Key.Semicolon;    break;
            case VK_OEM_PLUS:   mapped_key = Key.Equal;        break;
            case VK_OEM_4:      mapped_key = Key.LeftBracket;  break;
            case VK_OEM_5:      mapped_key = Key.BackSlash;    break;
            case VK_OEM_6:      mapped_key = Key.RightBracket; break;
            case VK_OEM_3:      mapped_key = Key.Backtick;     break;
            case VK_ESCAPE:     mapped_key = Key.Escape;       break;
            case VK_RETURN:     mapped_key = Key.Enter;        break;
            case VK_TAB:        mapped_key = Key.Tab;          break;
            case VK_BACK:       mapped_key = Key.Backspace;    break;
            case VK_INSERT:     mapped_key = Key.Insert;       break;
            case VK_DELETE:     mapped_key = Key.Delete;       break;
            case VK_RIGHT:      mapped_key = Key.Right;        break;
            case VK_LEFT:       mapped_key = Key.Left;         break;
            case VK_DOWN:       mapped_key = Key.Down;         break;
            case VK_UP:         mapped_key = Key.Up;           break;
            case VK_PRIOR:      mapped_key = Key.PageUp;       break;
            case VK_NEXT:       mapped_key = Key.PageDown;     break;
            case VK_HOME:       mapped_key = Key.Home;         break;
            case VK_END:        mapped_key = Key.End;          break;
            case VK_CAPITAL:    mapped_key = Key.Capslock;     break;
            case VK_SCROLL:     mapped_key = Key.ScrollLock;   break;
            case VK_NUMLOCK:    mapped_key = Key.NumLock;      break;
            case VK_SNAPSHOT:   mapped_key = Key.PrintScreen;  break;
            case VK_PAUSE:      mapped_key = Key.PauseBreak;   break;
            case VK_F1:         mapped_key = Key.F1;           break;
            case VK_F2:         mapped_key = Key.F2;           break;
            case VK_F3:         mapped_key = Key.F3;           break;
            case VK_F4:         mapped_key = Key.F4;           break;
            case VK_F5:         mapped_key = Key.F5;           break;
            case VK_F6:         mapped_key = Key.F6;           break;
            case VK_F7:         mapped_key = Key.F7;           break;
            case VK_F8:         mapped_key = Key.F8;           break;
            case VK_F9:         mapped_key = Key.F9;           break;
            case VK_F10:        mapped_key = Key.F10;          break;
            case VK_F11:        mapped_key = Key.F11;          break;
            case VK_F12:        mapped_key = Key.F12;          break;
            case VK_NUMPAD0:    mapped_key = Key.Numpad0;      break;
            case VK_NUMPAD1:    mapped_key = Key.Numpad1;      break;
            case VK_NUMPAD2:    mapped_key = Key.Numpad2;      break;
            case VK_NUMPAD3:    mapped_key = Key.Numpad3;      break;
            case VK_NUMPAD4:    mapped_key = Key.Numpad4;      break;
            case VK_NUMPAD5:    mapped_key = Key.Numpad5;      break;
            case VK_NUMPAD6:    mapped_key = Key.Numpad6;      break;
            case VK_NUMPAD7:    mapped_key = Key.Numpad7;      break;
            case VK_NUMPAD8:    mapped_key = Key.Numpad8;      break;
            case VK_NUMPAD9:    mapped_key = Key.Numpad9;      break;
            case VK_DECIMAL:    mapped_key = Key.Decimal;      break;
            case VK_DIVIDE:     mapped_key = Key.Divide;       break;
            case VK_MULTIPLY:   mapped_key = Key.Multiply;     break;
            case VK_SUBTRACT:   mapped_key = Key.Subtract;     break;
            case VK_ADD:        mapped_key = Key.Add;          break;
            // TODO: fix special VK_KEYS with left right variants
            case VK_SHIFT:
            case VK_LSHIFT:     mapped_key = Key.LeftShift;    break;
            case VK_LCONTROL:   mapped_key = Key.LeftCtrl;     break;
            case VK_LMENU:      mapped_key = Key.LeftAlt;      break;
            case VK_LWIN:       mapped_key = Key.LeftSuper;    break;
            case VK_RSHIFT:     mapped_key = Key.RightShift;   break;
            case VK_RCONTROL:   mapped_key = Key.RightCtrl;    break;
            case VK_RMENU:      mapped_key = Key.RightAlt;     break;
            case VK_RWIN:       mapped_key = Key.RightSuper;   break;
        }
        key[mapped_key].setPressed( (!was_down) & ( is_down));
        key[mapped_key].setHeld(    ( was_down) & ( is_down));
        key[mapped_key].setReleased(( was_down) & (!is_down));
    }
}

interface USR extends User32 {
    USR INST = (USR)Native.load("user32", USR.class, W32APIOptions.DEFAULT_OPTIONS);

    int CS_VREDRAW = 0x0001;
    int CS_HREDRAW = 0x0002;
    int PM_REMOVE  = 0x0001;

    WinDef.BOOL SetProcessDPIAware();
    HCURSOR LoadCursorW(HINSTANCE hInstance, int lpCursorName);
    int ShowCursor(WinDef.BOOL bShow);
}

interface GDI extends StdCallLibrary {
    GDI INST = (GDI)Native.load("gdi32", GDI.class, W32APIOptions.DEFAULT_OPTIONS);

    int StretchDIBits(WinDef.HDC hdcDest, int xDest, int yDest, int DestWidth, int DestHeight, int xSrc, int ySrc,
            int SrcWidth, int SrcHeight, Pointer lpBits, WinGDI.BITMAPINFO lpbmi, int iUsage, int rop);
}

class Key{
    public static final int
        Space = 32,
        Quotes = 39,
        Comma = 44,
        Minus = 45,
        Period = 46,
        FrontSlash = 47,
        Semicolon = 59,
        Equal = 61,
        LeftBracket = 91,
        BackSlash = 92,
        RightBracket = 93,
        Backtick = 96,
        Escape = 256,
        Enter = 257,
        Tab = 258,
        Backspace = 259,
        Insert = 260,
        Delete = 261,
        Right = 262,
        Left = 263,
        Down = 264,
        Up = 265,
        PageUp = 266,
        PageDown = 267,
        Home = 268,
        End = 269,
        Capslock = 280,
        ScrollLock = 281,
        NumLock = 282,
        PrintScreen = 283,
        PauseBreak = 284,
        F1 = 290,
        F2 = 291,
        F3 = 292,
        F4 = 293,
        F5 = 294,
        F6 = 295,
        F7 = 296,
        F8 = 297,
        F9 = 298,
        F10 = 299,
        F11 = 300,
        F12 = 301,
        Numpad0 = 320,
        Numpad1 = 321,
        Numpad2 = 322,
        Numpad3 = 323,
        Numpad4 = 324,
        Numpad5 = 325,
        Numpad6 = 326,
        Numpad7 = 327,
        Numpad8 = 328,
        Numpad9 = 329,
        Decimal = 330,
        Divide = 331,
        Multiply = 332,
        Subtract = 333,
        Add = 334,
        LeftShift = 340,
        LeftCtrl = 341,
        LeftAlt = 342,
        LeftSuper = 343,
        RightShift = 344,
        RightCtrl = 345,
        RightAlt = 346,
        RightSuper = 347;
    boolean pressed;
    boolean held;
    boolean released;
    public void setPressed(boolean key_state) { pressed = key_state; }
    public void setHeld(boolean key_state) { held = key_state; }
    public void setReleased(boolean key_state) { released = key_state; }
    public boolean isPressed() { return pressed; }
    public boolean isHeld() { return held; }
    public boolean isReleased() { return released; }
    public Key() {
        pressed = false;
        held = false;
        released = false;
    }
}

