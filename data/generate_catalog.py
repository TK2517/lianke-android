"""Build the small offline starter catalog from USDA FoodData Central releases.

Source archives are downloaded from https://fdc.nal.usda.gov/download-datasets/.
Numbers are per 100 g of the stated food form. Chinese labels are UI translations,
not claims that the source food matches a particular Chinese brand or recipe.
"""
from pathlib import Path
import csv
import json
import zipfile

BASE = Path(__file__).resolve().parent
OUT = BASE.parent / "app/src/main/assets/foods.tsv"
SOURCES = {
    "F26": ("foundation-2026-04.zip", "FoundationFoods", "USDA Foundation 2026-04"),
    "SR18": ("sr-legacy-2018-04.zip", "SRLegacyFoods", "USDA SR Legacy 2018-04"),
}
# (source, FDC ID, Chinese display label, category)
PICKS = [
    ("SR18", 168935, "米饭·白米熟", "主食"),
    ("SR18", 169704, "糙米饭·熟", "主食"),
    ("F26", 2512381, "白米·生", "主食"),
    ("F26", 2346396, "燕麦片·干", "主食"),
    ("SR18", 168919, "鸡蛋面·熟", "主食"),
    ("SR18", 168928, "意大利面·熟", "主食"),
    ("SR18", 174924, "白面包", "主食"),
    ("SR18", 170093, "烤土豆·带皮", "主食"),
    ("F26", 2346404, "红薯·生", "主食"),
    ("SR18", 173424, "鸡蛋·水煮", "蛋奶"),
    ("F26", 748967, "鸡蛋·生", "蛋奶"),
    ("F26", 746782, "全脂牛奶", "蛋奶"),
    ("F26", 746778, "低脂牛奶", "蛋奶"),
    ("F26", 330137, "希腊酸奶·无脂原味", "蛋奶"),
    ("F26", 2259793, "原味酸奶·全脂", "蛋奶"),
    ("SR18", 173414, "切达奶酪", "蛋奶"),
    ("SR18", 171477, "鸡胸肉·熟烤", "肉鱼豆"),
    ("F26", 2646170, "鸡胸肉·生", "肉鱼豆"),
    ("F26", 2646169, "猪里脊·生", "肉鱼豆"),
    ("F26", 2514743, "牛肉末·生·90%瘦", "肉鱼豆"),
    ("SR18", 175168, "三文鱼·熟", "肉鱼豆"),
    ("SR18", 175180, "虾·熟", "肉鱼豆"),
    ("SR18", 172448, "豆腐·硬", "肉鱼豆"),
    ("SR18", 172449, "豆腐·嫩", "肉鱼豆"),
    ("SR18", 173735, "黑豆·熟", "肉鱼豆"),
    ("SR18", 172421, "小扁豆·熟", "肉鱼豆"),
    ("F26", 1105314, "香蕉·生", "水果"),
    ("SR18", 171688, "苹果·带皮", "水果"),
    ("F26", 746771, "橙子·生", "水果"),
    ("F26", 2346398, "菠萝·生", "水果"),
    ("F26", 747447, "西兰花·生", "蔬菜"),
    ("F26", 2258586, "胡萝卜·生", "蔬菜"),
    ("F26", 1999634, "番茄·生", "蔬菜"),
    ("F26", 2346406, "黄瓜·带皮生", "蔬菜"),
    ("SR18", 169975, "卷心菜·生", "蔬菜"),
    ("F26", 1999633, "菠菜·生", "蔬菜"),
    ("SR18", 169999, "甜玉米·熟", "蔬菜"),
    ("F26", 2346393, "杏仁·生", "坚果油脂"),
    ("SR18", 172430, "花生·生", "坚果油脂"),
    ("SR18", 171413, "橄榄油", "坚果油脂"),
    ("SR18", 171411, "大豆油", "坚果油脂"),
    ("SR18", 174277, "酱油", "调味饮料"),
    ("SR18", 171890, "黑咖啡", "调味饮料"),
    ("F26", 1999630, "无糖豆奶", "调味饮料"),
]


def load_archive(filename, key):
    with zipfile.ZipFile(BASE / filename) as archive:
        return {item["fdcId"]: item for item in json.loads(archive.read(archive.namelist()[0]))[key] if item}


def amount(food, nutrient_id):
    for item in food["foodNutrients"]:
        if item["nutrient"]["id"] == nutrient_id and item.get("amount") is not None:
            return float(item["amount"])
    return None


def main():
    indexed = {code: load_archive(filename, key) for code, (filename, key, _) in SOURCES.items()}
    rows = []
    for code, fdc_id, chinese, category in PICKS:
        food = indexed[code][fdc_id]
        energy = next((value for nid in (1008, 2048, 2047) if (value := amount(food, nid)) is not None), None)
        nutrients = [amount(food, nid) for nid in (1003, 1005, 1004)]
        if energy is None or any(value is None for value in nutrients):
            raise ValueError(f"Missing required nutrient: {fdc_id} {food['description']}")
        rows.append([fdc_id, chinese, food["description"], category,
                     *[f"{value:.2f}" for value in (energy, *nutrients)],
                     f"{SOURCES[code][2]} · FDC {fdc_id}"])
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t")
        writer.writerow(["fdc_id", "name", "english", "category", "kcal", "protein", "carb", "fat", "source"])
        writer.writerows(rows)
    print(f"Wrote {len(rows)} foods to {OUT}")


if __name__ == "__main__":
    main()
