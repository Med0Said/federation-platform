import {
  MAT_SELECT_CONFIG,
  MAT_SELECT_SCROLL_STRATEGY,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER_FACTORY,
  MAT_SELECT_TRIGGER,
  MatSelect,
  MatSelectChange,
  MatSelectModule,
  MatSelectTrigger
} from "./chunk-LVYVECQX.js";
import "./chunk-Y6J72RWC.js";
import "./chunk-2S7IES64.js";
import {
  MatError,
  MatFormField,
  MatHint,
  MatLabel,
  MatPrefix,
  MatSuffix
} from "./chunk-YTABSCYA.js";
import "./chunk-JP3TR66W.js";
import "./chunk-VEVXYS3R.js";
import "./chunk-S4VAQ4SO.js";
import "./chunk-SVYNCHRR.js";
import "./chunk-4VBWOBU6.js";
import {
  MatOptgroup,
  MatOption
} from "./chunk-HORELF3I.js";
import "./chunk-R4QB32YE.js";
import "./chunk-AABGZPM5.js";
import "./chunk-PQVE7UYQ.js";
import "./chunk-5OWECNHR.js";
import "./chunk-OSRRUTVM.js";
import "./chunk-RLLJCJ3P.js";
import "./chunk-JXBCBRYI.js";
import "./chunk-42FJBLFI.js";
import "./chunk-HY3YTBJJ.js";
import "./chunk-DMGYNBAD.js";
import "./chunk-2O4WY5GE.js";
import "./chunk-BIGK7GFW.js";
import "./chunk-4UBEPPRW.js";
import "./chunk-KWB6JEQH.js";
import "./chunk-43U6UUQ6.js";
import "./chunk-J6YSA62T.js";
import "./chunk-PV7DW26G.js";
import "./chunk-XVJVK3DV.js";
import "./chunk-IVSRFTZX.js";

// node_modules/@angular/material/fesm2022/select.mjs
var matSelectAnimations = {
  // Represents
  // trigger('transformPanelWrap', [
  //   transition('* => void', query('@transformPanel', [animateChild()], {optional: true})),
  // ])
  /**
   * This animation ensures the select's overlay panel animation (transformPanel) is called when
   * closing the select.
   * This is needed due to https://github.com/angular/angular/issues/23302
   */
  transformPanelWrap: {
    type: 7,
    name: "transformPanelWrap",
    definitions: [{
      type: 1,
      expr: "* => void",
      animation: {
        type: 11,
        selector: "@transformPanel",
        animation: [{
          type: 9,
          options: null
        }],
        options: {
          optional: true
        }
      },
      options: null
    }],
    options: {}
  },
  // Represents
  // trigger('transformPanel', [
  //   state(
  //     'void',
  //     style({
  //       opacity: 0,
  //       transform: 'scale(1, 0.8)',
  //     }),
  //   ),
  //   transition(
  //     'void => showing',
  //     animate(
  //       '120ms cubic-bezier(0, 0, 0.2, 1)',
  //       style({
  //         opacity: 1,
  //         transform: 'scale(1, 1)',
  //       }),
  //     ),
  //   ),
  //   transition('* => void', animate('100ms linear', style({opacity: 0}))),
  // ])
  /** This animation transforms the select's overlay panel on and off the page. */
  transformPanel: {
    type: 7,
    name: "transformPanel",
    definitions: [{
      type: 0,
      name: "void",
      styles: {
        type: 6,
        styles: {
          opacity: 0,
          transform: "scale(1, 0.8)"
        },
        offset: null
      }
    }, {
      type: 1,
      expr: "void => showing",
      animation: {
        type: 4,
        styles: {
          type: 6,
          styles: {
            opacity: 1,
            transform: "scale(1, 1)"
          },
          offset: null
        },
        timings: "120ms cubic-bezier(0, 0, 0.2, 1)"
      },
      options: null
    }, {
      type: 1,
      expr: "* => void",
      animation: {
        type: 4,
        styles: {
          type: 6,
          styles: {
            opacity: 0
          },
          offset: null
        },
        timings: "100ms linear"
      },
      options: null
    }],
    options: {}
  }
};
export {
  MAT_SELECT_CONFIG,
  MAT_SELECT_SCROLL_STRATEGY,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER_FACTORY,
  MAT_SELECT_TRIGGER,
  MatError,
  MatFormField,
  MatHint,
  MatLabel,
  MatOptgroup,
  MatOption,
  MatPrefix,
  MatSelect,
  MatSelectChange,
  MatSelectModule,
  MatSelectTrigger,
  MatSuffix,
  matSelectAnimations
};
//# sourceMappingURL=@angular_material_select.js.map
