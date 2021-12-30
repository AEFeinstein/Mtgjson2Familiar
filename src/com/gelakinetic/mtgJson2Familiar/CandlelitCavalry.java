package com.gelakinetic.mtgJson2Familiar;

import com.gelakinetic.GathererScraper.JsonTypes.Card;

import java.util.ArrayList;

public class CandlelitCavalry extends Card {
    public CandlelitCavalry() {
        this.mName = "Candlelit Cavalry";
        this.mManaCost = "{4}{G}";
        this.mSortedManaCost = "{4}{G}";
        this.mCmc = 5;
        this.mType = "Creature - Human Knight";
        this.mText = "Coven - At the beginning of combat on your turn, if you control three or more creatures with different powers, Candlelit Cavalry gains trample until end of turn.";
        this.mFlavor = "\"On this night, the dark will fear us.\"";
        this.mExpansion = "MID";
        this.mScryfallSetCode = "MID";
        this.mRarity = 'C';
        this.mNumber = "175";
        this.mArtist = "Viko Menezes";
        this.mColor = "G";
        this.mColorIdentity = "G";
        this.mMultiverseId = 534961;
        this.mPower = 5.0f;
        this.mToughness = 5.0f;
        this.mLoyalty = -1005;
        this.mTcgplayerProductId = 247852;
        this.mForeignPrintings = new ArrayList<>();
        this.mForeignPrintings.add(new ForeignPrinting("de", "Kerzenschein-Kavallerie"));
        this.mForeignPrintings.add(new ForeignPrinting("es", "Caballería alumbrada con velas"));
        this.mForeignPrintings.add(new ForeignPrinting("fr", "Cavalerie aux chandelles"));
        this.mForeignPrintings.add(new ForeignPrinting("it", "Cavalleria a Lume di Candela"));
        this.mForeignPrintings.add(new ForeignPrinting("ja", "蝋燭明かりの騎兵"));
        this.mForeignPrintings.add(new ForeignPrinting("ko", "촛불을 밝힌 기병대"));
        this.mForeignPrintings.add(new ForeignPrinting("pt_BR", "Cavalaria à Luz de Velas"));
        this.mForeignPrintings.add(new ForeignPrinting("ru", "Свечная Кавалерия"));
        this.mForeignPrintings.add(new ForeignPrinting("zh_HANS", "烛光骑兵"));
        this.mForeignPrintings.add(new ForeignPrinting("zh_HANT", "燭光騎兵"));
    }
}
