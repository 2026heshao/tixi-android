#!/usr/bin/env python3
"""重建内置宋体子集（Noto Serif SC → res/font/noto_serif_sc_{400,500}.ttf）。

为什么要子集：全量 Noto Serif SC 单字重 ~14.8 MB，两个字重 30 MB 进包不可接受。
子集目标 = GB2312 汉字 6763 + 拉丁/希腊/数学/全角标点/圈号等区块 + App 源码里出现的全部字符。
子集外的生僻字由系统字体回退链兜底（不会出现豆腐块），只是字形风格会略有差异。

用法（在工程根目录）：
    python -m pip install fonttools brotli
    python tools/make_font_subset.py

依赖外网：字体从 jsDelivr 拉 @expo-google-fonts/noto-serif-sc（Google Fonts 全量静态实例）。
字体许可 SIL OFL-1.1，见 app/src/main/assets/licenses/OFL-NotoSerifSC.txt。
"""
from __future__ import annotations

import glob
import os
import subprocess
import sys
import urllib.request

WEIGHTS = (400, 500)
BASE_URL = (
    "https://cdn.jsdelivr.net/npm/@expo-google-fonts/noto-serif-sc@0.4.1/"
    "{w}{name}/NotoSerifSC_{w}{name}.ttf"
)
NAMES = {400: "Regular", 500: "Medium"}

# 逐区块收录：ASCII、拉丁补充、常用标点、货币、字母式符号、箭头、数学算子、
# 圈号、几何图形、杂项符号、CJK 标点、全角形式
BLOCKS = [
    (0x20, 0x7E), (0xA0, 0xFF), (0x2000, 0x206F), (0x20A0, 0x20BF),
    (0x2100, 0x214F), (0x2190, 0x21FF), (0x2200, 0x22FF), (0x2460, 0x24FF),
    (0x25A0, 0x25FF), (0x2600, 0x26FF), (0x3000, 0x303F), (0xFF00, 0xFFEF),
]

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORK = os.path.join(ROOT, ".build-tmp", "font")
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")
SRC_DIR = os.path.join(ROOT, "app", "src", "main")


def build_charset() -> str:
    codes: set[int] = set()
    for start, end in BLOCKS:
        codes.update(range(start, end + 1))

    # GB2312 全部汉字（6763 个），够覆盖教材与常见题库
    for hi in range(0xB0, 0xF8):
        for lo in range(0xA1, 0xFF):
            try:
                codes.add(ord(bytes([hi, lo]).decode("gb2312")))
            except UnicodeDecodeError:
                continue
    codes.add(0x3007)  # 〇

    # App 源码里出现的所有非 ASCII 字符，避免漏掉自己写的文案用字
    for pattern in ("**/*.kt", "**/*.xml"):
        for path in glob.glob(os.path.join(SRC_DIR, pattern), recursive=True):
            with open(path, encoding="utf-8", errors="ignore") as fp:
                for ch in fp.read():
                    if ord(ch) > 0x7F:
                        codes.add(ord(ch))

    hanzi = sum(1 for c in codes if 0x4E00 <= c <= 0x9FFF)
    print(f"目标码位 {len(codes)}（其中汉字 {hanzi}）")
    return ",".join(f"U+{c:04X}" for c in sorted(codes))


def download(weight: int) -> str:
    dst = os.path.join(WORK, f"full-{weight}.ttf")
    if os.path.exists(dst) and os.path.getsize(dst) > 10_000_000:
        return dst
    url = BASE_URL.format(w=weight, name=NAMES[weight])
    print(f"下载 {url}")
    urllib.request.urlretrieve(url, dst)
    return dst


def main() -> int:
    os.makedirs(WORK, exist_ok=True)
    os.makedirs(OUT_DIR, exist_ok=True)
    charset_path = os.path.join(WORK, "charset.txt")
    with open(charset_path, "w", encoding="utf-8") as fp:
        fp.write(build_charset())

    for weight in WEIGHTS:
        src = download(weight)
        out = os.path.join(OUT_DIR, f"noto_serif_sc_{weight}.ttf")
        cmd = [
            sys.executable, "-m", "fontTools.subset", src,
            f"--unicodes-file={charset_path}",
            "--layout-features=*",
            "--drop-tables+=VORG,vhea,vmtx,DSIG",
            "--no-hinting",
            "--name-IDs=*",
            f"--output-file={out}",
        ]
        subprocess.run(cmd, check=True)
        print(f"{os.path.basename(out)}  {os.path.getsize(out) / 1048576:.2f} MB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
