Android-CircularView
====================

This view is intended to display Circular view composed of 2 circles. Inner circle to show standard content cropped into
circle, and outer to display PieChart-like portions of added items. Added items should be of any type (subclass of Object
in future only parcelable objects to save its state?) and contains following description (See CircularView.ItemDescriptor
class):
* score : score of item. As result item will have drawn its portion of outer circle computed as:

```item.score / scoreSum * 360°```

* paint color: color in which outer circle will display percentage of current item

This is first version of readme file, so no other code examples are presented then in code sample (see app module).

![BOW Diagram](image1.png)
