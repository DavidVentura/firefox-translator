#!/usr/bin/env python3
"""
Generate Kotlin enum class for language pairs from model repository structure.
Usage: python generate_language_enum.py <repository_path>
"""

import os
import sys
import json
import asyncio
import aiohttp
from pathlib import Path
from typing import Dict, Set, Tuple

COMMIT = "a06d4724eb95d7452f9251cf2cc4ca2706636d74"
TRANSLATION_BASE_URL = f"https://media.githubusercontent.com/media/mozilla/firefox-translations-models/{COMMIT}/models"
TESSERACT_BASE_URL = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/refs/heads/main"
DICTIONARY_BASE_URL = "https://translator.davidv.dev/dictionaries"
DICT_VERSION = 1

# Language code to display name mapping
LANGUAGE_NAMES = {
    'ar': 'Arabic',
    'az': 'Azerbaijani',
    'be': 'Belarusian',
    'bg': 'Bulgarian',
    'bn': 'Bengali',
    'bs': 'Bosnian',
    'ca': 'Catalan',
    'cs': 'Czech',
    'da': 'Danish',
    'de': 'German',
    'el': 'Greek',
    'en': 'English',
    'es': 'Spanish',
    'et': 'Estonian',
    'fa': 'Persian',
    'fi': 'Finnish',
    'fr': 'French',
    'gu': 'Gujarati',
    'he': 'Hebrew',
    'hi': 'Hindi',
    'hr': 'Croatian',
    'hu': 'Hungarian',
    'id': 'Indonesian',
    'is': 'Icelandic',
    'it': 'Italian',
    'ja': 'Japanese',
    'kn': 'Kannada',
    'ko': 'Korean',
    'lt': 'Lithuanian',
    'lv': 'Latvian',
    'ml': 'Malayalam',
    'ms': 'Malay',
    'mt': 'Maltese',
    'nb': 'Norwegian Bokmål',
    'nl': 'Dutch',
    'nn': 'Norwegian Nynorsk',
    'pl': 'Polish',
    'pt': 'Portuguese',
    'ro': 'Romanian',
    'ru': 'Russian',
    'sk': 'Slovak',
    'sl': 'Slovenian',
    'sq': 'Albanian',
    'sr': 'Serbian',
    'sv': 'Swedish',
    'ta': 'Tamil',
    'te': 'Telugu',
    'tr': 'Turkish',
    'uk': 'Ukrainian',
    'vi': 'Vietnamese',
    'zh': 'Chinese'
}

TESSERACT_LANGUAGE_MAPPINGS = {
    'ar': 'ara',          # Arabic
    'az': 'aze',          # Azerbaijani
    'be': 'bel',          # Belarusian
    'bg': 'bul',          # Bulgarian
    'bn': 'ben',          # Bengali
    'bs': 'bos',          # Bosnian
    'ca': 'cat',          # Catalan
    'cs': 'ces',          # Czech
    'da': 'dan',          # Danish
    'de': 'deu',          # German
    'el': 'ell',          # Greek
    'en': 'eng',          # English
    'es': 'spa',          # Spanish
    'et': 'est',          # Estonian
    'fa': 'fas',          # Persian
    'fi': 'fin',          # Finnish
    'fr': 'fra',          # French
    'gu': 'guj',          # Gujarati
    'he': 'heb',          # Hebrew
    'hi': 'hin',          # Hindi
    'hr': 'hrv',          # Croatian
    'hu': 'hun',          # Hungarian
    'id': 'ind',          # Indonesian
    'is': 'isl',          # Icelandic
    'it': 'ita',          # Italian
    'ja': 'jpn',          # Japanese
    'kn': 'kan',          # Kannada
    'ko': 'kor',          # Korean
    'lt': 'lit',          # Lithuanian
    'lv': 'lav',          # Latvian
    'ml': 'mal',          # Malayalam
    'ms': 'msa',          # Malay
    'mt': 'mlt',          # Maltese
    'nb': 'nor',          # Norwegian Bokmål (using nor for Norwegian)
    'nl': 'nld',          # Dutch
    'nn': 'nor',          # Norwegian Nynorsk (using nor for Norwegian)
    'pl': 'pol',          # Polish
    'pt': 'por',          # Portuguese
    'ro': 'ron',          # Romanian
    'ru': 'rus',          # Russian
    'sk': 'slk',          # Slovak
    'sl': 'slv',          # Slovenian
    'sq': 'sqi',          # Albanian
    'sr': 'srp',          # Serbian
    'sv': 'swe',          # Swedish
    'ta': 'tam',          # Tamil
    'te': 'tel',          # Telugu
    'tr': 'tur',          # Turkish
    'uk': 'ukr',          # Ukrainian
    'vi': 'vie',          # Vietnamese
    'zh': 'chi_sim',      # Chinese (using simplified Chinese as default)
}

# Script mappings for transliteration (ICU4J script names)
LANGUAGE_SCRIPTS = {
    'ar': 'Arabic',       # Arabic script
    'az': 'Latin',        # Azerbaijan uses Latin script (post-1991)
    'be': 'Cyrillic',     # Belarusian uses Cyrillic
    'bg': 'Cyrillic',     # Bulgarian uses Cyrillic
    'bn': 'Bengali',      # Bengali script (also called Bangla)
    'bs': 'Latin',        # Bosnian uses Latin script
    'ca': 'Latin',        # Catalan uses Latin
    'cs': 'Latin',        # Czech uses Latin
    'da': 'Latin',        # Danish uses Latin
    'de': 'Latin',        # German uses Latin
    'el': 'Greek',        # Greek script
    'en': 'Latin',        # English uses Latin
    'es': 'Latin',        # Spanish uses Latin
    'et': 'Latin',        # Estonian uses Latin
    'fa': 'Arabic',       # Persian uses Arabic script (modified)
    'fi': 'Latin',        # Finnish uses Latin
    'fr': 'Latin',        # French uses Latin
    'gu': 'Gujarati',     # Gujarati script
    'he': 'Hebrew',       # Hebrew script
    'hi': 'Devanagari',   # Hindi uses Devanagari
    'hr': 'Latin',        # Croatian uses Latin
    'hu': 'Latin',        # Hungarian uses Latin
    'id': 'Latin',        # Indonesian uses Latin
    'is': 'Latin',        # Icelandic uses Latin
    'it': 'Latin',        # Italian uses Latin
    'ja': 'Japanese',     # Japanese (mixed: Hiragana, Katakana, Han)
    'kn': 'Kannada',      # Kannada script
    'ko': 'Hangul',       # Korean uses Hangul
    'lt': 'Latin',        # Lithuanian uses Latin
    'lv': 'Latin',        # Latvian uses Latin
    'ml': 'Malayalam',    # Malayalam script
    'ms': 'Latin',        # Malay uses Latin
    'mt': 'Latin',        # Maltese uses Latin
    'nb': 'Latin',        # Norwegian Bokmål uses Latin
    'nl': 'Latin',        # Dutch uses Latin
    'nn': 'Latin',        # Norwegian Nynorsk uses Latin
    'pl': 'Latin',        # Polish uses Latin
    'pt': 'Latin',        # Portuguese uses Latin
    'ro': 'Latin',        # Romanian uses Latin
    'ru': 'Cyrillic',     # Russian uses Cyrillic
    'sk': 'Latin',        # Slovak uses Latin
    'sl': 'Latin',        # Slovenian uses Latin
    'sq': 'Latin',        # Albanian uses Latin
    'sr': 'Cyrillic',     # Serbian primarily uses Cyrillic
    'sv': 'Latin',        # Swedish uses Latin
    'ta': 'Tamil',        # Tamil script
    'te': 'Telugu',       # Telugu script
    'tr': 'Latin',        # Turkish uses Latin
    'uk': 'Cyrillic',     # Ukrainian uses Cyrillic
    'vi': 'Latin',        # Vietnamese uses Latin
    'zh': 'Han',          # Chinese uses Han characters
}

def extract_language_pairs(repo_path: Path) -> Dict[str, Set[str]]:
    """
    Extract language pairs from repository structure.
    Returns dict mapping model_type -> set of language pairs
    Validates that all found pairs are in the supported list.
    """
    language_pairs = {'base': set(), 'base-memory': set(), 'tiny': set()}

    # Scan models/base, models/base-memory, and models/tiny directories
    for model_type in ['base', 'base-memory', 'tiny']:
        models_dir = repo_path / 'models' / model_type
        if models_dir.exists():
            for item in models_dir.iterdir():
                if item.is_dir():
                    # Directory name should be language pair (e.g., 'enar', 'enru')
                    pair_name = item.name
                    src, tgt = parse_language_pair(pair_name)
                    if src not in LANGUAGE_NAMES or tgt not in LANGUAGE_NAMES:
                        print(f"Error: Unsupported language pair '{pair_name}' found in {model_type}")
                        print(f"Supported pairs: {sorted(LANGUAGE_NAMES.keys())}")
                        sys.exit(1)
                    language_pairs[model_type].add(pair_name)

    return language_pairs

def parse_language_pair(pair: str) -> Tuple[str, str]:
    """
    Parse language pair string to extract source and target language codes.
    Asserts the pair is exactly 4 characters and splits it in half.
    """
    if len(pair) != 4:
        print(f"Error: Language pair '{pair}' must be exactly 4 characters")
        sys.exit(1)

    # Split the 4-character pair in half
    src = pair[:2]
    tgt = pair[2:]

    return src, tgt

def get_non_english_language(pair: str) -> Tuple[str, str]:
    """
    Get the non-English language code and name from a language pair.
    Returns (language_code, language_name)
    Validates that the language code exists in LANGUAGE_NAMES.
    """
    src, tgt = parse_language_pair(pair)

    if src == 'en':
        lang_code = tgt
    elif tgt == 'en':
        lang_code = src
    else:
        # If neither is English, return the target language
        lang_code = tgt

    if lang_code not in LANGUAGE_NAMES:
        print(f"Error: Language code '{lang_code}' from pair '{pair}' not found in supported language names")
        print(f"Supported language codes: {sorted(LANGUAGE_NAMES.keys())}")
        sys.exit(1)

    return lang_code, LANGUAGE_NAMES[lang_code]

def get_best_model_type(model_types: Set[str]) -> str:
    """
    Get the best model type based on priority: base-memory > base > tiny
    """
    if 'base-memory' in model_types:
        return 'base-memory'
    elif 'base' in model_types:
        return 'base'
    elif 'tiny' in model_types:
        return 'tiny'
    else:
        raise ValueError(f"No valid model type found in {model_types}")

def generate_files_for_language(from_code: str, to_code: str) -> Dict[str, str]:
    """
    Generate file paths for a language pair based on the Kotlin filesFor logic.
    Returns dict with keys: model, lex, srcVocab, tgtVocab
    """
    lang_pair = f"{from_code}{to_code}"
    model = f"model.{lang_pair}.intgemm.alphas.bin"
    lex = f"lex.50.50.{lang_pair}.s2t.bin"

    # Split vocab for Chinese and Japanese
    split_vocab_langs = {'zh', 'ja', 'ko'}

    if to_code in split_vocab_langs:
        src_vocab = f"srcvocab.{from_code}{to_code}.spm"
        tgt_vocab = f"trgvocab.{from_code}{to_code}.spm"
    else:
        vocab_file = f"vocab.{lang_pair}.spm"
        src_vocab = vocab_file
        tgt_vocab = vocab_file

    return {
        'model': model,
        'lex': lex,
        'srcVocab': src_vocab,
        'tgtVocab': tgt_vocab
    }

def generate_kotlin_enum(language_pairs: Dict[str, Set[str]], existing_sizes: dict[str, dict[str, int]]) -> str:
    """
    Generate Kotlin enum classes for Language and LanguagePair.
    """
    # Collect all unique languages
    all_languages = set()
    for pairs in language_pairs.values():
        for pair in pairs:
            src_code, tgt_code = parse_language_pair(pair)
            all_languages.add(src_code)
            all_languages.add(tgt_code)

    all_languages.add("en")

    # Collect all unique language pairs with their model types
    pairs_data = {}  # pair -> (src_code, tgt_code, model_types)

    for model_type, pairs in language_pairs.items():
        for pair in pairs:
            src_code, tgt_code = parse_language_pair(pair)

            # Assert that pairs are only to or from English
            if src_code != 'en' and tgt_code != 'en':
                print(f"Error: Language pair '{pair}' is not to or from English")
                print(f"Only English-to-X or X-to-English pairs are supported")
                sys.exit(1)

            if pair not in pairs_data:
                pairs_data[pair] = (src_code, tgt_code, set())
            pairs_data[pair][2].add(model_type)

    # Separate into fromEnglish and toEnglish
    from_english = {}  # lang_code -> model_type
    to_english = {}    # lang_code -> model_type

    for pair, (src_code, tgt_code, model_types) in pairs_data.items():
        best_model_type = get_best_model_type(model_types)

        if src_code == 'en':
            # English to other language
            from_english[tgt_code] = best_model_type
        else:
            # Other language to English
            to_english[src_code] = best_model_type

    # Ensure 2 way translation
    from_english = {k: v for k, v in from_english.items() if k in to_english}
    to_english = {k: v for k, v in to_english.items() if k in from_english}

    # Get sizes
    all_lang_codes = set(from_english.keys())
    all_lang_codes.add('en')

    for lang_code in sorted(all_lang_codes):
        if lang_code not in existing_sizes:
            print(f"Fetching size for {lang_code}")
            existing_sizes[lang_code] = asyncio.run(get_language_sizes(lang_code, language_pairs))
            save_sizes(existing_sizes)

    # Generate Language enum entries
    language_entries = []
    for lang_code in sorted(all_languages):
        # from_english is bidirectional with to_english
        if lang_code not in from_english and lang_code != 'en':
          continue
        lang_name = LANGUAGE_NAMES[lang_code]
        tess_name = TESSERACT_LANGUAGE_MAPPINGS[lang_code]
        script = LANGUAGE_SCRIPTS[lang_code]
        enum_name = lang_name.upper().replace(' ', '_').replace('Å', 'A')

        sizes = existing_sizes[lang_code]
        tess_filename = f"{tess_name}.traineddata"
        tessdata_size = sizes[tess_filename]

        # full, including tessdata
        translation_size = sum(v for v in sizes.values())

        language_entries.append(f'    {enum_name}("{lang_code}", "{tess_name}", "{lang_name}", "{script}", {translation_size}, {tessdata_size})')

    language_entries = sorted(language_entries)

    # Generate fromEnglishFiles map entries
    from_english_files_entries = []
    for lang_code in sorted(from_english.keys()):
        lang_name = LANGUAGE_NAMES[lang_code]
        lang_enum = f'Language.{lang_name.upper().replace(" ", "_").replace("Å", "A")}'
        model_type_enum = f'ModelType.{from_english[lang_code].upper().replace("-", "_")}'
        files = generate_files_for_language('en', lang_code)
        sizes = existing_sizes[lang_code]

        model_size = sizes[files["model"]]
        src_vocab_size = sizes[files["srcVocab"]]
        tgt_vocab_size = sizes[files["tgtVocab"]]
        lex_size = sizes[files["lex"]]

        from_english_files_entries.append(f'    {lang_enum} to LanguageFiles(Pair("{files["model"]}", {model_size}), Pair("{files["srcVocab"]}", {src_vocab_size}), Pair("{files["tgtVocab"]}", {tgt_vocab_size}), Pair("{files["lex"]}", {lex_size}), {model_type_enum})')

    # Generate toEnglishFiles map entries
    to_english_files_entries = []
    for lang_code in sorted(to_english.keys()):
        lang_name = LANGUAGE_NAMES[lang_code]
        lang_enum = f'Language.{lang_name.upper().replace(" ", "_").replace("Å", "A")}'
        model_type_enum = f'ModelType.{to_english[lang_code].upper().replace("-", "_")}'
        files = generate_files_for_language(lang_code, 'en')
        sizes = existing_sizes[lang_code]

        model_size = sizes[files["model"]]
        src_vocab_size = sizes[files["srcVocab"]]
        tgt_vocab_size = sizes[files["tgtVocab"]]
        lex_size = sizes[files["lex"]]

        to_english_files_entries.append(f'    {lang_enum} to LanguageFiles(Pair("{files["model"]}", {model_size}), Pair("{files["srcVocab"]}", {src_vocab_size}), Pair("{files["tgtVocab"]}", {tgt_vocab_size}), Pair("{files["lex"]}", {lex_size}), {model_type_enum})')

    to_english_lines = ",\n".join(to_english_files_entries)
    from_english_lines = ",\n".join(from_english_files_entries)
    language_lines = ",\n".join(language_entries)
    # Generate the complete enum classes and maps
    kotlin_code = f"""/*
 * Copyright (C) 2024 David V
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

// This file was generated by `generated.py`. Do not edit.

package dev.davidv.translator

object Constants {{
  const val DICT_VERSION = {DICT_VERSION}
  const val DEFAULT_TRANSLATION_MODELS_BASE_URL =
    "{TRANSLATION_BASE_URL}"
  const val DEFAULT_TESSERACT_MODELS_BASE_URL = "{TESSERACT_BASE_URL}"
  const val DEFAULT_DICTIONARY_BASE_URL = "{DICTIONARY_BASE_URL}"
}}

enum class ModelType(private val pathName: String) {{
    BASE("base"),
    BASE_MEMORY("base-memory"),
    TINY("tiny");

    override fun toString(): String = pathName
}}

enum class Language(val code: String, val tessName: String, val displayName: String, val script: String, val sizeBytes: Int, val tessdataSizeBytes: Int) {{
{language_lines};

    val tessFilename: String
        get() = "$tessName.traineddata"
}}

data class LanguageFiles(
    val model: Pair<String, Int>,
    val srcVocab: Pair<String, Int>,
    val tgtVocab: Pair<String, Int>,
    val lex: Pair<String, Int>,
    val quality: ModelType
) {{
    fun allFiles(): List<String> = listOf(model.first, srcVocab.first, tgtVocab.first, lex.first).distinct()
}}

val fromEnglishFiles = mapOf(
{from_english_lines}
)

val toEnglishFiles = mapOf(
{to_english_lines}
)"""

    return kotlin_code

async def get_file_size(session: aiohttp.ClientSession, url: str) -> int:
    """Get file size using HTTP HEAD request."""
    try:
        async with session.head(url) as response:
            if response.status == 200:
                content_length = response.headers.get('Content-Length')
                if content_length:
                    return int(content_length)
    except Exception as e:
        print(f"Error getting size for {url}: {e}")
    return 0

async def get_language_sizes(lang_code: str, language_pairs: Dict[str, Set[str]]) -> dict:
    """Get sizes for all files related to a specific language."""
    sizes = {}

    async with aiohttp.ClientSession() as session:
        # Find pairs involving this language
        relevant_pairs = []
        for model_type, pairs in language_pairs.items():
            for pair in pairs:
                src_code, tgt_code = parse_language_pair(pair)
                non_en_lang = tgt_code if src_code == 'en' else src_code

                if non_en_lang == lang_code:
                    if pair not in [p[0] for p in relevant_pairs]:
                        relevant_pairs.append((pair, src_code, tgt_code, set()))

                    # Find the pair in our list and add this model type
                    for i, (p, s, t, model_types) in enumerate(relevant_pairs):
                        if p == pair:
                            relevant_pairs[i] = (p, s, t, model_types | {model_type})
                            break

        # Collect all URLs to fetch concurrently
        url_to_filename = {}

        # Get translation model URLs
        for pair, src_code, tgt_code, model_types in relevant_pairs:
            best_model_type = get_best_model_type(model_types)
            files = generate_files_for_language(src_code, tgt_code)

            for filename in sorted(set(files.values())):
                if filename not in sizes:
                    url = f"{TRANSLATION_BASE_URL}/{best_model_type}/{src_code}{tgt_code}/{filename}.gz"
                    url_to_filename[url] = filename

        # Get tesseract URL
        tess_name = TESSERACT_LANGUAGE_MAPPINGS[lang_code]
        tess_filename = f"{tess_name}.traineddata"
        tess_url = f"{TESSERACT_BASE_URL}/{tess_filename}"
        url_to_filename[tess_url] = tess_filename

        # Execute all HTTP HEAD requests concurrently
        tasks = [get_file_size(session, url) for url in url_to_filename.keys()]
        results = await asyncio.gather(*tasks)

        # Map results back to filenames
        for url, size in zip(url_to_filename.keys(), results):
            filename = url_to_filename[url]
            if size > 0:
                sizes[filename] = size

    return sizes

def load_existing_sizes() -> dict:
    """Load existing sizes from JSON file if it exists."""
    sizes_file = f"data/{COMMIT}.json"
    if os.path.exists(sizes_file):
        with open(sizes_file, 'r') as f:
            return json.load(f)
    return {}

def save_sizes(sizes: dict):
    """Save sizes to JSON file."""
    sizes_file = f"data/{COMMIT}.json"
    with open(sizes_file, 'w') as f:
        json.dump(sizes, f, indent=2, sort_keys=True)

def main():
    if len(sys.argv) != 2:
        print("Usage: python generate_language_enum.py <repository_path>")
        sys.exit(1)

    repo_path = sys.argv[1]

    if not os.path.exists(repo_path):
        print(f"Error: Repository path '{repo_path}' does not exist")
        sys.exit(1)

    print(f"Scanning repository at: {repo_path}")

    # Extract language pairs
    language_pairs = extract_language_pairs(Path(repo_path))

    existing_sizes = load_existing_sizes()
    print(f"Found {len(language_pairs['base'])} base language pairs")
    print(f"Found {len(language_pairs['base-memory'])} base-memory language pairs")
    print(f"Found {len(language_pairs['tiny'])} tiny language pairs")

    if not language_pairs['base'] and not language_pairs['base-memory'] and not language_pairs['tiny']:
        print("No language pairs found. Please check the repository structure.")
        sys.exit(1)

    # Generate Kotlin enum
    kotlin_code = generate_kotlin_enum(language_pairs, existing_sizes)

    # Write to file
    output_file = "Language.kt"
    with open(output_file, 'w') as f:
        f.write(kotlin_code)

    print(f"Generated Kotlin enum class in {output_file}")
    print("\nPreview:")
    print(kotlin_code)

if __name__ == "__main__":
    main()
