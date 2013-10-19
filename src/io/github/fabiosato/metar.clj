(ns io.github.fabiosato.metar
  (:require [instaparse.core :as insta])
  (:require [clojure.java.io :as io]))


(def metar-parser
  (insta/parser
    "REPORT = TYPE? AIRPORT DATETIME AUTO? WIND WIND_VAR? VIS VIS_VAR? VIS_RWY* WEATHER* CLOUD* TEMPERATURE PRESSURE END
    TYPE = (METAR | SPECI) <sp>
    METAR = <'METAR'>
    SPECI = <'SPECI'>
    sp = #\"\\s*\"
    AIRPORT = #\"[A-Z]{4}\" <sp>
    DATETIME = (DAY_OF_MONTH)(TIME) <sp>
    DAY_OF_MONTH = #\"\\d{2}\"
    TIME = HOUR MINUTE <\"Z\">
    HOUR = #\"\\d{2}\"
    MINUTE = #\"\\d{2}\"
    AUTO = <\"AUTO\"> <sp>
    WIND = (WIND_DIR)(WIND_SPEED) <sp>
    WIND_DIR = #\"\\d{3}\"|\"VRB\"
    WIND_SPEED = (WIND_UNIT_OVERFLOW* <#\"\\d{2}\">|#\"\\d{2}\") WIND_GUST* WIND_UNIT
    WIND_UNIT_OVERFLOW = <\"P\">
    WIND_GUST = <'G'> (WIND_UNIT_OVERFLOW* <#\"[0-9]{2}\">|#\"[0-9]{2}\")
    WIND_UNIT = \"MPS\"|\"KT\"|\"KMH\"
    WIND_VAR = WIND_FROM 'V' WIND_TO <sp>
    WIND_FROM = #\"[0-9]{3}\"
    WIND_TO = #\"[0-9]{3}\"
    VIS = (#\"[0-9]{2}\" STATUTE_MILES | #\"[0-9]\"'/'#\"[0-9]\" STATUTE_MILES | #\"[0-9]{4}\" NDV? | CAVOK) <sp>
    STATUTE_MILES = <'SM'>
    NDV = <\"NDV\">
    CAVOK = <\"CAVOK\">
    VIS_VAR = #\"[0-9]{4}\" COMPASS_DIR <sp>
    COMPASS_DIR = 'N'| 'S' | 'W' | 'E' | 'NE' | 'SE' | 'SW' | 'NW'
    VIS_RWY = RWY_ID <'/'> (RWY_VIS | RWY_VIS_VAR) <sp>
    RWY_ID = <'R'> #\"[0-9]{2}\" ('L'|'C'|'R')?
    RWY_VIS = (PLUS|MINUS)? #\"\\d{4}\" DISTINCT_TENDENCY?
    DISTINCT_TENDENCY = DOWNWARD | UPWARD | NO
    DOWNWARD = 'D'
    UPWARD = 'U'
    NO = 'N'
    RWY_VIS_VAR = RWY_VIS_MIN <'V'> RWY_VIS_MAX DISTINCT_TENDENCY?
    RWY_VIS_MIN = MINUS? #\"[0-9]{4}\"
    RWY_VIS_MAX = PLUS? #\"[0-9]{4}\"
    PLUS = <'+'>|<'P'>
    MINUS = <'-'>|<'M'>
    WEATHER = ((([INTENSITY]|[VICINITY])[DESCRIPTOR]PRECIPITATION+)|OBSCURATION|OTHER) <sp>
    INTENSITY = LIGHT | HEAVY
    LIGHT = '-'
    HEAVY = '+'
    VICINITY = 'VC'
    DESCRIPTOR = SHALLOW | PATCHES | PARTIAL | LOW_DRIFTING | BLOWING | SHOWER | THUNDERSTORM | FREEZING
    SHALLOW = 'MI'
    PATCHES = 'BC'
    PARTIAL = 'PR'
    LOW_DRIFTING = 'DR'
    BLOWING = 'BL'
    SHOWER = 'SH'
    THUNDERSTORM = 'TS'
    FREEZING = 'FZ'
    PRECIPITATION = DRIZZLE | RAIN | SNOW | SNOW_GRAINS | ICE_CRYSTALS | ICE_PELLETS | HAIL | SMALL_HAIL_OR_SNOW_PELLETS | UNKNOWN_PRECIPITATION
    DRIZZLE = 'DZ'
    RAIN = 'RA'
    SNOW = 'SN'
    SNOW_GRAINS = 'SG'
    ICE_CRYSTALS = 'IC'
    ICE_PELLETS = 'PL'
    HAIL = 'GR'
    SMALL_HAIL_OR_SNOW_PELLETS = 'GS'
    UNKNOWN_PRECIPITATION = 'UP'
    OBSCURATION = MIST | FOG | SMOKE | VOLCANIC_ASH | WIDESPREAD_DUST | SAND | HAZE
    MIST = 'BR'
    FOG = 'FG'
    SMOKE = 'FU'
    VOLCANIC_ASH = 'VA'
    WIDESPREAD_DUST = 'DU'
    SAND = 'SA'
    HAZE = 'HZ'
    OTHER = DUST_OR_SAND_WHIRLS | SQUALLS | FUNNEL_CLOUD | SANDSTORM | DUSTSTORM
    DUST_OR_SAND_WHIRLS = 'PO'
    SQUALLS = 'SQ'
    FUNNEL_CLOUD = 'FC'
    SANDSTORM = 'SS'
    DUSTSTORM = 'DS'
    CLOUD = (((CLOUD_AMOUNT CLOUD_HEIGHT) | NO_CLOUD | CLOUD_TYPE) | VERTICAL_VIS | CAVOK ) <sp>
    CANNOT_OBSERVE = '///'
    CLOUD_AMOUNT = FEW | SCATTERED | BROKEN | OVERCAST | CANNOT_OBSERVE
    CLOUD_HEIGHT = #\"\\d{3}\" | CANNOT_OBSERVE
    CLOUD_TYPE = CUMULONIMBUS | TOWERING_CUMULUS | CANNOT_OBSERVE
    NO_CLOUD = 'NSC' | 'NCD'
    CUMULONIMBUS = 'CB'
    TOWERING_CUMULUS = 'TCU'
    FEW = 'FEW'
    SCATTERED = 'SCT'
    BROKEN = 'BKN' | 'BNK'
    OVERCAST = 'OVC'
    NO_CLOUDS = 'NSC' |'NCD'
    VERTICAL_VIS = <'VV'> (#\"\\d{3}\" | CANNOT_OBSERVE)
    TEMPERATURE = AIR_TEMPERATURE <'/'> DEWPOINT_TEMPERATURE <sp>
    AIR_TEMPERATURE = MINUS? #\"\\d{2}\"
    DEWPOINT_TEMPERATURE = MINUS? #\"\\d{2}\"
    PRESSURE = hPA | inHg
    hPA = <'Q'> #\"\\d{4}\"
    inHg = <'A'> #\"\\d{4}\" (* americans really like to mess up with units *)
    END = #\".*\" '='?
    "
    ))

(def testcode "METAR LBBG 041600Z 12003MPS 310V290 1400 R04/P1500N R22/P1500U +SN BKN022 OVC050 M04/M07 Q1020 NOSIG 9949//91=")
(def testcode2 "METAR SBFL 120000Z 03008KT 9999 FEW020 BKN035 21/17 Q1020=")

(defn process-metar-file
  "Process a metar data file. f can be either a URL/URI, filename or File object."
  [f]
    (with-open [rdr (io/reader f)]
      (doseq [line (line-seq rdr)]
        (println (metar-parser line)))))

(defn -main  [& args]
  (doseq [fname args]
    (doseq [m (process-metar-file fname)] (println m))))
