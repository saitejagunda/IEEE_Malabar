package com.ieeemalabar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.ieeemalabar.models.User;
import com.ieeemalabar.models.Comment;
import com.ieeemalabar.models.Post;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends BaseActivity {

    private static final String TAG = "PostDetailActivity";

    public static final String EXTRA_POST_KEY = "post_key";

    private DatabaseReference mPostReference;
    private DatabaseReference mCommentsReference;
    private ValueEventListener mPostListener;
    public static String mPostKey;
    private CommentAdapter mAdapter;

    private TextView mAuthorView;
    private TextView mDateView;
    private TextView mCollegeView;
    private TextView mTitleView;
    private TextView mBodyView;
    private EditText mCommentField;
    private RecyclerView mCommentsRecycler;

    private ImageView mFeatured;

    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;
    StorageReference imageRef;
    StorageReference profRef;
    public static Bitmap bmp;
    public static Bitmap profBmp;
    Post post;
    String userId;
    public static String title;
    public static PostDetailActivity PostDA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        PostDA = this;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Get post key from intent
        mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);

        if (mPostKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_POST_KEY");
        }

        userId = getUid();
        //Toast toast = Toast.makeText(PostDetailActivity.this, userId, Toast.LENGTH_SHORT);
        //toast.show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSupportActionBar().setElevation(0);
        }

        // Initialize Database
        mPostReference = FirebaseDatabase.getInstance().getReference().child("posts").child(mPostKey);
        mCommentsReference = FirebaseDatabase.getInstance().getReference().child("post-comments").child(mPostKey);

        mStorage = FirebaseStorage.getInstance();
        mStorageRef = mStorage.getReferenceFromUrl("gs://project-3576505284407387518.appspot.com/");

        imageRef = mStorageRef.child("featured/" + mPostKey + ".jpg");

        // Initialize Views
        mAuthorView = (TextView) findViewById(R.id.post_author);
        mDateView = (TextView) findViewById(R.id.post_date);
        mCollegeView = (TextView) findViewById(R.id.collegeView);
        mTitleView = (TextView) findViewById(R.id.post_title);
        mBodyView = (TextView) findViewById(R.id.post_body);
        mFeatured = (ImageView) findViewById(R.id.feature);
        mCommentField = (EditText) findViewById(R.id.field_comment_text);
        mCommentsRecycler = (RecyclerView) findViewById(R.id.recycler_comments);
        mCommentsRecycler.setNestedScrollingEnabled(false);
        mCommentField.setOnTouchListener(new View.OnTouchListener() {
                                             @Override
                                             public boolean onTouch(View v, MotionEvent event) {
                                                 if (v.getId() == R.id.field_comment_text) {
                                                     v.getParent().requestDisallowInterceptTouchEvent(true);
                                                     switch (event.getAction() & MotionEvent.ACTION_MASK) {
                                                         case MotionEvent.ACTION_UP:
                                                             v.getParent().requestDisallowInterceptTouchEvent(false);
                                                             break;
                                                     }
                                                 }
                                                 return false;
                                             }
                                         }

        );
        ScrollView scroll = (ScrollView) findViewById(R.id.post_details_scroll);
        scroll.setOnTouchListener(new View.OnTouchListener() {
                                      @Override
                                      public boolean onTouch(View v, MotionEvent event) {
                                          if (mCommentField.hasFocus()) {
                                              mCommentField.clearFocus();
                                          }
                                          return false;
                                      }
                                  }

        );

        mCommentField.addTextChangedListener(new TextWatcher() {
                                                 @Override
                                                 public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                                 }

                                                 @Override
                                                 public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                     if (s.toString().trim().length() == 0) {
                                                         findViewById(R.id.submit_comment).setVisibility(View.GONE);
                                                     } else {
                                                         findViewById(R.id.submit_comment).setVisibility(View.VISIBLE);
                                                     }
                                                 }

                                                 @Override
                                                 public void afterTextChanged(Editable s) {
                                                 }
                                             }

        );

        findViewById(R.id.submit_comment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String commentText = mCommentField.getText().toString().trim();
                if (commentText.length() > 0)
                    postComment();
            }
        });

        LinearLayoutManager mManager;
        mManager = new LinearLayoutManager(this);
        mManager.setReverseLayout(true);
        mManager.setStackFromEnd(true);
        mCommentsRecycler.setLayoutManager(mManager);
        //mCommentsRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onStart() {
        super.onStart();
        RelativeLayout f_layout = (RelativeLayout) findViewById(R.id.f_layout);
        f_layout.setVisibility(View.VISIBLE);
        // Add value event listener to the post
        // [START post_value_event_listener]
        mPostListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                post = dataSnapshot.getValue(Post.class);
                // [START_EXCLUDE]
                mAuthorView.setText(post.author);
                mDateView.setText(post.date);
                mCollegeView.setText(post.college);
                mTitleView.setText(post.title.substring(0, 1).toUpperCase() + post.title.substring(1));
                mBodyView.setText(post.body);
                // [END_EXCLUDE]

                loadProfileImage(post.uid);

                if (userId.equals(post.uid)) {
                    RelativeLayout post_edit = (RelativeLayout) findViewById(R.id.post_edit);
                    post_edit.setVisibility(View.VISIBLE);
                    post_edit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Toast.makeText(getActivity(), "Edit Clicked", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(PostDetailActivity.this, NewPostActivity.class);
                            intent.putExtra("condition", "edit");
                            intent.putExtra("post_key", mPostKey);
                            intent.putExtra("college", post.college);
                            startActivity(intent);
                        }
                    });
                }

                final long ONE_MEGABYTE = 1024 * 1024;
                imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        //Toast.makeText(PostDetailActivity.this, "Invalid Name", Toast.LENGTH_SHORT).show()
                        bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        mFeatured.setImageBitmap(bmp);
                        mFeatured.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(new Intent(PostDetailActivity.this, ViewImage.class));
                            }
                        });
                        title = post.title;
                        ProgressBar image_load = (ProgressBar) findViewById(R.id.image_load);
                        image_load.setVisibility(View.GONE);

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(PostDetailActivity.this, "Failed to load post.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        mPostReference.addValueEventListener(mPostListener);
        // [END post_value_event_listener]

        // Listen for comments
        mAdapter = new CommentAdapter(this, mCommentsReference);
        mCommentsRecycler.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Remove post value event listener
        if (mPostListener != null) {
            mPostReference.removeEventListener(mPostListener);
        }

        // Clean up comments listener
        mAdapter.cleanupListener();
    }

    private void postComment() {
        final String uid = getUid();
        FirebaseDatabase.getInstance().getReference().child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user information
                        User user = dataSnapshot.getValue(User.class);
                        String authorName = user.name;

                        // Create new comment object
                        String commentText = mCommentField.getText().toString().trim();
                        Comment comment = new Comment(uid, authorName, commentText);

                        // Push the comment, it will appear in the list
                        if (commentText.trim().length() > 0)
                            mCommentsReference.push().setValue(comment);

                        // Clear the field
                        mCommentField.setText(null);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private class CommentViewHolder extends RecyclerView.ViewHolder {

        public TextView authorView;
        public TextView bodyView;
        public ImageView comment_photo;
        public RelativeLayout commentLayout;

        public CommentViewHolder(View itemView) {
            super(itemView);
            authorView = (TextView) itemView.findViewById(R.id.comment_author);
            bodyView = (TextView) itemView.findViewById(R.id.comment_body);
            comment_photo = (ImageView) itemView.findViewById(R.id.comment_photo);
            commentLayout = (RelativeLayout) itemView.findViewById(R.id.commentLayout);
        }
    }

    private class CommentAdapter extends RecyclerView.Adapter<CommentViewHolder> {

        private Context mContext;
        private DatabaseReference mDatabaseReference;
        private ChildEventListener mChildEventListener;

        private List<String> mCommentIds = new ArrayList<>();
        private List<Comment> mComments = new ArrayList<>();

        public StorageReference mStorageRef;
        private FirebaseStorage mStorage;
        StorageReference profRef;
        ImageView profile;
        RelativeLayout CLayout;

        public CommentAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            // Create child event listener
            // [START child_event_listener_recycler]
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());
                    // A new comment has been added, add it to the displayed list
                    Comment comment = dataSnapshot.getValue(Comment.class);
                    // [START_EXCLUDE]
                    // Update RecyclerView
                    mCommentIds.add(dataSnapshot.getKey());
                    mComments.add(comment);
                    notifyItemInserted(mComments.size() - 1);
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                    // A comment has changed, use the key to determine if we are displaying this
                    // comment and if so displayed the changed comment.
                    Comment newComment = dataSnapshot.getValue(Comment.class);
                    String commentKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int commentIndex = mCommentIds.indexOf(commentKey);
                    if (commentIndex > -1) {
                        // Replace with the new data
                        mComments.set(commentIndex, newComment);

                        // Update the RecyclerView
                        notifyItemChanged(commentIndex);
                    } else {
                        Log.w(TAG, "onChildChanged:unknown_child:" + commentKey);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                    // A comment has changed, use the key to determine if we are displaying this
                    // comment and if so remove it.
                    String commentKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int commentIndex = mCommentIds.indexOf(commentKey);
                    if (commentIndex > -1) {
                        // Remove data from the list
                        mCommentIds.remove(commentIndex);
                        mComments.remove(commentIndex);

                        // Update the RecyclerView
                        notifyItemRemoved(commentIndex);
                    } else {
                        Log.w(TAG, "onChildRemoved:unknown_child:" + commentKey);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());

                    // A comment has changed position, use the key to determine if we are
                    // displaying this comment and if so move it.
                    Comment movedComment = dataSnapshot.getValue(Comment.class);
                    String commentKey = dataSnapshot.getKey();

                    // ...
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                    Toast.makeText(mContext, "Failed to load comments.",
                            Toast.LENGTH_SHORT).show();
                }
            };
            ref.addChildEventListener(mChildEventListener);
            // [END child_event_listener_recycler]
        }

        @Override
        public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final CommentViewHolder holder, int position) {
            final Comment comment = mComments.get(position);
            holder.authorView.setText(comment.author);
            holder.bodyView.setText(comment.text);

            mStorage = FirebaseStorage.getInstance();
            mStorageRef = mStorage.getReferenceFromUrl("gs://project-3576505284407387518.appspot.com/");
            profRef = mStorageRef.child("profile/" + comment.uid + ".jpg");
            final long ONE_MEGABYTE = 1024 * 1024;
            profRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap CommentBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    holder.comment_photo.setImageBitmap(CommentBmp);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    holder.comment_photo.setImageResource(R.drawable.prof_thumb);
                }
            });

            holder.commentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(PostDetailActivity.this, Profile.class);
                    if (comment.uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        intent.putExtra("message", "me");
                    } else {
                        intent.putExtra("message", "comment");
                    }
                    intent.putExtra("name", comment.author);
                    intent.putExtra("uid", comment.uid);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mComments.size();
        }

        public void cleanupListener() {
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

    }

    @Override
    public void onBackPressed() {
        //startActivity(new Intent(getApplicationContext(), MainContainer.class));
        finish();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }

    public void loadProfileImage(String uid) {
        final long ONE_MEGABYTE = 1024 * 1024;
        profRef = mStorageRef.child("profile/" + uid + ".jpg");
        profRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                //Toast.makeText(PostDetailActivity.this, "Invalid Name", Toast.LENGTH_SHORT).show()
                profBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                ImageView prof_pic = (ImageView) findViewById(R.id.post_author_photo);
                prof_pic.setImageBitmap(profBmp);

                LinearLayout user = (LinearLayout) findViewById(R.id.user);
                user.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(PostDetailActivity.this, Profile.class);

                        if (post.uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                            intent.putExtra("message", "me");
                        } else {
                            intent.putExtra("message", "details");
                        }

                        intent.putExtra("name", post.author);
                        intent.putExtra("college", post.college);
                        intent.putExtra("uid", post.uid);
                        startActivity(intent);
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

                ImageView prof_pic = (ImageView) findViewById(R.id.post_author_photo);
                prof_pic.setImageResource(R.drawable.prof_thumb);

                LinearLayout user = (LinearLayout) findViewById(R.id.user);
                user.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(PostDetailActivity.this, Profile.class);

                        if (post.uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                            intent.putExtra("message", "me");
                        } else {
                            intent.putExtra("message", "message");
                        }
                        intent.putExtra("name", post.author);
                        intent.putExtra("college", post.college);
                        intent.putExtra("uid", post.uid);
                        startActivity(intent);
                    }
                });
            }
        });
    }
}
